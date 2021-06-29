/*
 * Copyright 2014 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.parser.csv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.DataException;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInput;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.Schema;
import org.embulk.spi.type.TimestampType;
import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.Task;
import org.embulk.util.config.units.ColumnConfig;
import org.embulk.util.config.units.SchemaConfig;
import org.embulk.util.json.JsonParseException;
import org.embulk.util.json.JsonParser;
import org.embulk.util.text.LineDecoder;
import org.embulk.util.text.LineDelimiter;
import org.embulk.util.text.Newline;
import org.embulk.util.timestamp.TimestampFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvParserPlugin implements ParserPlugin {
    private static final Set<String> TRUE_STRINGS =
            Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                    "true", "True", "TRUE",
                    "yes", "Yes", "YES",
                    "t", "T", "y", "Y",
                    "on", "On", "ON",
                    "1")));

    public interface PluginTask extends Task {
        @Config("columns")
        SchemaConfig getSchemaConfig();

        @Config("header_line")
        @ConfigDefault("null")
        Optional<Boolean> getHeaderLine();

        @Config("skip_header_lines")
        @ConfigDefault("0")
        int getSkipHeaderLines();

        void setSkipHeaderLines(int n);

        @Config("delimiter")
        @ConfigDefault("\",\"")
        String getDelimiter();

        @Config("quote")
        @ConfigDefault("\"\\\"\"")
        Optional<QuoteCharacter> getQuoteChar();

        @Config("escape")
        @ConfigDefault("\"\\\\\"")
        Optional<EscapeCharacter> getEscapeChar();

        // Null value handling: if the CsvParser found 'non-quoted empty string's,
        // it replaces them to string that users specified like "\N", "NULL".
        @Config("null_string")
        @ConfigDefault("null")
        Optional<String> getNullString();

        @Config("trim_if_not_quoted")
        @ConfigDefault("false")
        boolean getTrimIfNotQuoted();

        @Config("quotes_in_quoted_fields")
        @ConfigDefault("\"ACCEPT_ONLY_RFC4180_ESCAPED\"")
        QuotesInQuotedFields getQuotesInQuotedFields();

        @Config("max_quoted_size_limit")
        @ConfigDefault("131072") //128kB
        long getMaxQuotedSizeLimit();

        @Config("comment_line_marker")
        @ConfigDefault("null")
        Optional<String> getCommentLineMarker();

        @Config("allow_optional_columns")
        @ConfigDefault("false")
        boolean getAllowOptionalColumns();

        @Config("allow_extra_columns")
        @ConfigDefault("false")
        boolean getAllowExtraColumns();

        @Config("stop_on_invalid_record")
        @ConfigDefault("false")
        boolean getStopOnInvalidRecord();

        // From org.embulk.spi.util.LineDecoder.DecoderTask.
        @Config("charset")
        @ConfigDefault("\"utf-8\"")
        Charset getCharset();

        // From org.embulk.spi.util.LineDecoder.DecoderTask.
        @Config("newline")
        @ConfigDefault("\"CRLF\"")
        Newline getNewline();

        // From org.embulk.spi.util.LineDecoder.DecoderTask.
        @Config("line_delimiter_recognized")
        @ConfigDefault("null")
        Optional<LineDelimiter> getLineDelimiterRecognized();

        // From org.embulk.spi.time.TimestampParser.Task.
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        String getDefaultTimeZoneId();

        // From org.embulk.spi.time.TimestampParser.Task.
        @Config("default_timestamp_format")
        @ConfigDefault("\"%Y-%m-%d %H:%M:%S.%N %z\"")
        String getDefaultTimestampFormat();

        // From org.embulk.spi.time.TimestampParser.Task.
        @Config("default_date")
        @ConfigDefault("\"1970-01-01\"")
        String getDefaultDate();
    }

    public enum QuotesInQuotedFields {
        ACCEPT_ONLY_RFC4180_ESCAPED,
        ACCEPT_STRAY_QUOTES_ASSUMING_NO_DELIMITERS_IN_FIELDS,
        ;

        @JsonCreator
        public static QuotesInQuotedFields ofString(final String string) {
            for (final QuotesInQuotedFields value : values()) {
                if (string.equals(value.toString())) {
                    return value;
                }
            }
            throw new ConfigException("\"quotes_in_quoted_fields\" must be one of [ACCEPT_ONLY_RFC4180_ESCAPED, ACCEPT_STRAY_QUOTES_ASSUMING_NO_DELIMITERS_IN_FIELDS].");
        }
    }

    public static class QuoteCharacter {
        private final char character;

        public QuoteCharacter(char character) {
            this.character = character;
        }

        public static QuoteCharacter noQuote() {
            return new QuoteCharacter(CsvTokenizer.NO_QUOTE);
        }

        @JsonCreator
        @SuppressWarnings("checkstyle:LineLength")
        public static QuoteCharacter ofString(String str) {
            if (str.length() >= 2) {
                throw new ConfigException("\"quote\" option accepts only 1 character.");
            } else if (str.isEmpty()) {
                logger.warn("Setting '' (empty string) to \"quote\" option is obsoleted. Currently it becomes '\"' automatically but this behavior will be removed. Please set '\"' explicitly.");
                return new QuoteCharacter('"');
            } else {
                return new QuoteCharacter(str.charAt(0));
            }
        }

        @JsonIgnore
        public char getCharacter() {
            return character;
        }

        @JsonValue
        public String getOptionalString() {
            return new String(new char[] {character});
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof QuoteCharacter)) {
                return false;
            }
            QuoteCharacter o = (QuoteCharacter) obj;
            return character == o.character;
        }

        @Override
        public int hashCode() {
            return Objects.hash(character);
        }
    }

    public static class EscapeCharacter {
        private final char character;

        public EscapeCharacter(char character) {
            this.character = character;
        }

        public static EscapeCharacter noEscape() {
            return new EscapeCharacter(CsvTokenizer.NO_ESCAPE);
        }

        @JsonCreator
        @SuppressWarnings("checkstyle:LineLength")
        public static EscapeCharacter ofString(String str) {
            if (str.length() >= 2) {
                throw new ConfigException("\"escape\" option accepts only 1 character.");
            } else if (str.isEmpty()) {
                logger.warn("Setting '' (empty string) to \"escape\" option is obsoleted. Currently it becomes null automatically but this behavior will be removed. Please set \"escape: null\" explicitly.");
                return noEscape();
            } else {
                return new EscapeCharacter(str.charAt(0));
            }
        }

        @JsonIgnore
        public char getCharacter() {
            return character;
        }

        @JsonValue
        public String getOptionalString() {
            return new String(new char[] {character});
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof EscapeCharacter)) {
                return false;
            }
            EscapeCharacter o = (EscapeCharacter) obj;
            return character == o.character;
        }

        @Override
        public int hashCode() {
            return Objects.hash(character);
        }
    }

    private interface TimestampColumnOption extends org.embulk.util.config.Task {
        @Config("timezone")
        @ConfigDefault("null")
        Optional<String> getTimeZoneId();

        @Config("format")
        @ConfigDefault("null")
        Optional<String> getFormat();

        @Config("date")
        @ConfigDefault("null")
        Optional<String> getDate();
    }

    public CsvParserPlugin() {
    }

    @Override
    @SuppressWarnings("deprecation")  // For the use of task#dump().
    public void transaction(ConfigSource config, ParserPlugin.Control control) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, PluginTask.class);

        // backward compatibility
        if (task.getHeaderLine().isPresent()) {
            if (task.getSkipHeaderLines() > 0) {
                throw new ConfigException("'header_line' option is invalid if 'skip_header_lines' is set.");
            }
            if (task.getHeaderLine().get()) {
                task.setSkipHeaderLines(1);
            } else {
                task.setSkipHeaderLines(0);
            }
        }

        control.run(task.dump(), task.getSchemaConfig().toSchema());
    }

    @Override
    public void run(TaskSource taskSource, final Schema schema,
            FileInput input, PageOutput output) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createTaskMapper().map(taskSource, PluginTask.class);
        final TimestampFormatter[] timestampFormatters = newTimestampColumnFormatters(task, task.getSchemaConfig());
        final JsonParser jsonParser = new JsonParser();
        final CsvTokenizer tokenizer = new CsvTokenizer(
                LineDecoder.of(input, task.getCharset(), task.getLineDelimiterRecognized().orElse(null)), task);
        final boolean allowOptionalColumns = task.getAllowOptionalColumns();
        final boolean allowExtraColumns = task.getAllowExtraColumns();
        final boolean stopOnInvalidRecord = task.getStopOnInvalidRecord();
        final int skipHeaderLines = task.getSkipHeaderLines();

        try (final PageBuilder pageBuilder = getPageBuilder(Exec.getBufferAllocator(), schema, output)) {
            while (tokenizer.nextFile()) {
                final String fileName = input.hintOfCurrentInputFileNameForLogging().orElse("-");

                // skip the header lines for each file
                for (int skipHeaderLineNumber = skipHeaderLines; skipHeaderLineNumber > 0; skipHeaderLineNumber--) {
                    if (!tokenizer.skipHeaderLine()) {
                        break;
                    }
                }

                if (!tokenizer.nextRecord()) {
                    // empty file
                    continue;
                }

                while (true) {
                    boolean hasNextRecord;

                    try {
                        schema.visitColumns(new ColumnVisitor() {
                                public void booleanColumn(Column column) {
                                    String v = nextColumn();
                                    if (v == null) {
                                        pageBuilder.setNull(column);
                                    } else {
                                        pageBuilder.setBoolean(column, TRUE_STRINGS.contains(v));
                                    }
                                }

                                public void longColumn(Column column) {
                                    String v = nextColumn();
                                    if (v == null) {
                                        pageBuilder.setNull(column);
                                    } else {
                                        try {
                                            pageBuilder.setLong(column, Long.parseLong(v));
                                        } catch (NumberFormatException e) {
                                            // TODO support default value
                                            throw new CsvRecordValidateException(e);
                                        }
                                    }
                                }

                                public void doubleColumn(Column column) {
                                    String v = nextColumn();
                                    if (v == null) {
                                        pageBuilder.setNull(column);
                                    } else {
                                        try {
                                            pageBuilder.setDouble(column, Double.parseDouble(v));
                                        } catch (NumberFormatException e) {
                                            // TODO support default value
                                            throw new CsvRecordValidateException(e);
                                        }
                                    }
                                }

                                public void stringColumn(Column column) {
                                    String v = nextColumn();
                                    if (v == null) {
                                        pageBuilder.setNull(column);
                                    } else {
                                        pageBuilder.setString(column, v);
                                    }
                                }

                                public void timestampColumn(Column column) {
                                    String v = nextColumn();
                                    if (v == null) {
                                        pageBuilder.setNull(column);
                                    } else {
                                        final Instant instant;
                                        try {
                                            instant = timestampFormatters[column.getIndex()].parse(v);
                                        } catch (final DateTimeParseException e) {
                                            // TODO support default value
                                            throw new CsvRecordValidateException(e);
                                        }
                                        setTimestamp(pageBuilder, column, instant);
                                    }
                                }

                                public void jsonColumn(Column column) {
                                    String v = nextColumn();
                                    if (v == null) {
                                        pageBuilder.setNull(column);
                                    } else {
                                        try {
                                            pageBuilder.setJson(column, jsonParser.parse(v));
                                        } catch (JsonParseException e) {
                                            // TODO support default value
                                            throw new CsvRecordValidateException(e);
                                        }
                                    }
                                }

                                private String nextColumn() {
                                    if (allowOptionalColumns && !tokenizer.hasNextColumn()) {
                                        // TODO warning
                                        return null;
                                    }
                                    return tokenizer.nextColumnOrNull();
                                }
                            });

                        try {
                            hasNextRecord = tokenizer.nextRecord();
                        } catch (CsvTokenizer.TooManyColumnsException ex) {
                            if (allowExtraColumns) {
                                String tooManyColumnsLine = tokenizer.skipCurrentLine();
                                // TODO warning
                                hasNextRecord = tokenizer.nextRecord();
                            } else {
                                // this line will be skipped at the following catch section
                                throw ex;
                            }
                        }
                        pageBuilder.addRecord();

                    } catch (CsvTokenizer.InvalidFormatException | CsvTokenizer.InvalidValueException | CsvRecordValidateException e) {
                        String skippedLine = tokenizer.skipCurrentLine();
                        long lineNumber = tokenizer.getCurrentLineNumber();
                        if (stopOnInvalidRecord) {
                            throw new DataException(String.format("Invalid record at %s:%d: %s", fileName, lineNumber, skippedLine), e);
                        }
                        logger.warn(String.format("Skipped line %s:%d (%s): %s", fileName, lineNumber, e.getMessage(), skippedLine));
                        //exec.notice().skippedLine(skippedLine);

                        hasNextRecord = tokenizer.nextRecord();
                    }

                    if (!hasNextRecord) {
                        break;
                    }
                }
            }

            pageBuilder.finish();
        }
    }

    static class CsvRecordValidateException extends DataException {
        CsvRecordValidateException(Throwable cause) {
            super(cause);
        }
    }

    @SuppressWarnings("deprecation")  // For the use of new PageBuilder().
    private static PageBuilder getPageBuilder(final BufferAllocator allocator, final Schema schema, final PageOutput output) {
        try {
            return Exec.getPageBuilder(allocator, schema, output);
        } catch (final NoSuchMethodError ex) {
            // Exec.getPageBuilder() is available from v0.10.17, and "new PageBuidler()" is deprecated then.
            // It is not expected to happen because this plugin is embedded with Embulk v0.10.24+, but falling back just in case.
            // TODO: Remove this fallback in v0.11.
            logger.warn("embulk-parser-csv is expected to work with Embulk v0.10.17+.", ex);
            return new PageBuilder(allocator, schema, output);
        }
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1289
    private static TimestampFormatter[] newTimestampColumnFormatters(
            final PluginTask task,
            final SchemaConfig schema) {
        final TimestampFormatter[] formatters = new TimestampFormatter[schema.getColumnCount()];
        int i = 0;
        for (final ColumnConfig column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                final TimestampColumnOption columnOption =
                        CONFIG_MAPPER_FACTORY.createConfigMapper().map(column.getOption(), TimestampColumnOption.class);

                final String pattern = columnOption.getFormat().orElse(task.getDefaultTimestampFormat());
                formatters[i] = TimestampFormatter.builder(pattern, true)
                        .setDefaultZoneFromString(columnOption.getTimeZoneId().orElse(task.getDefaultTimeZoneId()))
                        .setDefaultDateFromString(columnOption.getDate().orElse(task.getDefaultDate()))
                        .build();
            }
            i++;
        }
        return formatters;
    }

    @SuppressWarnings("deprecation")  // For the use of new PageBuilder with java.time.Instant.
    private static void setTimestamp(final PageBuilder pageBuilder, final Column column, final Instant instant) {
        try {
            pageBuilder.setTimestamp(column, instant);
        } catch (final NoSuchMethodError ex) {
            // PageBuilder with Instant is available from v0.10.13, and org.embulk.spi.Timestamp is deprecated.
            // It is not expected to happen because this plugin is embedded with Embulk v0.10.24+, but falling back just in case.
            // TODO: Remove this fallback in v0.11.
            logger.warn("embulk-parser-csv is expected to work with Embulk v0.10.17+.", ex);
            pageBuilder.setTimestamp(column, org.embulk.spi.time.Timestamp.ofInstant(instant));
        }
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();

    private static final Logger logger = LoggerFactory.getLogger(CsvParserPlugin.class);
}
