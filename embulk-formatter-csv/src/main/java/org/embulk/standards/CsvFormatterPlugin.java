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

package org.embulk.standards;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.Exec;
import org.embulk.spi.FileOutput;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.type.TimestampType;
import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.Task;
import org.embulk.util.config.modules.CharsetModule;
import org.embulk.util.text.LineEncoder;
import org.embulk.util.text.Newline;
import org.embulk.util.timestamp.TimestampFormatter;
import org.msgpack.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvFormatterPlugin implements FormatterPlugin {
    public enum QuotePolicy {
        ALL("ALL"),
        MINIMAL("MINIMAL"),
        NONE("NONE");

        private final String string;

        private QuotePolicy(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }
    }

    public interface PluginTask extends Task {
        @Config("header_line")
        @ConfigDefault("true")
        boolean getHeaderLine();

        @Config("delimiter")
        @ConfigDefault("\",\"")
        char getDelimiterChar();

        @Config("quote")
        @ConfigDefault("\"\\\"\"")
        char getQuoteChar();

        @Config("quote_policy")
        @ConfigDefault("\"MINIMAL\"")
        QuotePolicy getQuotePolicy();

        @Config("escape")
        @ConfigDefault("null")
        Optional<Character> getEscapeChar();

        @Config("null_string")
        @ConfigDefault("\"\"")
        String getNullString();

        @Config("newline_in_field")
        @ConfigDefault("\"LF\"")
        Newline getNewlineInField();

        @Config("column_options")
        @ConfigDefault("{}")
        Map<String, TimestampColumnOption> getColumnOptions();

        // From org.embulk.spi.util.LineEncoder.EncoderTask.
        @Config("charset")
        @ConfigDefault("\"utf-8\"")
        Charset getCharset();

        // From org.embulk.spi.util.LineEncoder.EncoderTask.
        @Config("newline")
        @ConfigDefault("\"CRLF\"")
        Newline getNewline();

        // From org.embulk.spi.time.TimestampFormatter.Task.
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        String getDefaultTimeZoneId();

        // From org.embulk.spi.time.TimestampFormatter.Task.
        @Config("default_timestamp_format")
        @ConfigDefault("\"%Y-%m-%d %H:%M:%S.%6N %z\"")
        String getDefaultTimestampFormat();
    }

    public interface TimestampColumnOption extends Task {
        @Config("timezone")
        @ConfigDefault("null")
        Optional<String> getTimeZoneId();

        @Config("format")
        @ConfigDefault("null")
        Optional<String> getFormat();
    }

    @Override
    @SuppressWarnings("deprecation")  // For the use of task#dump().
    public void transaction(ConfigSource config, Schema schema, FormatterPlugin.Control control) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, PluginTask.class);

        // validate column_options
        for (String columnName : task.getColumnOptions().keySet()) {
            schema.lookupColumn(columnName);  // throws SchemaConfigException
        }

        control.run(task.dump());
    }

    @Override
    public PageOutput open(TaskSource taskSource, final Schema schema,
            FileOutput output) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createTaskMapper().map(taskSource, PluginTask.class);
        final LineEncoder encoder = LineEncoder.of(output, task.getNewline(), task.getCharset(), Exec.getBufferAllocator());
        final TimestampFormatter[] timestampFormatters = newTimestampColumnFormatters(task, schema, task.getColumnOptions());
        final char delimiter = task.getDelimiterChar();
        final QuotePolicy quotePolicy = task.getQuotePolicy();
        final char quote = task.getQuoteChar() != '\0' ? task.getQuoteChar() : '"';
        final char escape = task.getEscapeChar().orElse(quotePolicy == QuotePolicy.NONE ? '\\' : quote);
        final String newlineInField = task.getNewlineInField().getString();
        final String nullString = task.getNullString();

        // create a file
        encoder.nextFile();

        // write header
        if (task.getHeaderLine()) {
            writeHeader(schema, encoder, delimiter, quotePolicy, quote, escape, newlineInField, nullString);
        }

        return new PageOutput() {
            private final PageReader pageReader = getPageReader(schema);
            private final String delimiterString = String.valueOf(delimiter);

            public void add(Page page) {
                pageReader.setPage(page);
                while (pageReader.nextRecord()) {
                    schema.visitColumns(new ColumnVisitor() {
                            public void booleanColumn(Column column) {
                                addDelimiter(column);
                                if (!pageReader.isNull(column)) {
                                    addValue(Boolean.toString(pageReader.getBoolean(column)));
                                } else {
                                    addNullString();
                                }
                            }

                            public void longColumn(Column column) {
                                addDelimiter(column);
                                if (!pageReader.isNull(column)) {
                                    addValue(Long.toString(pageReader.getLong(column)));
                                } else {
                                    addNullString();
                                }
                            }

                            public void doubleColumn(Column column) {
                                addDelimiter(column);
                                if (!pageReader.isNull(column)) {
                                    addValue(Double.toString(pageReader.getDouble(column)));
                                } else {
                                    addNullString();
                                }
                            }

                            public void stringColumn(Column column) {
                                addDelimiter(column);
                                if (!pageReader.isNull(column)) {
                                    addValue(pageReader.getString(column));
                                } else {
                                    addNullString();
                                }
                            }

                            public void timestampColumn(Column column) {
                                addDelimiter(column);
                                if (!pageReader.isNull(column)) {
                                    addValue(this.formatTimestamp(column));
                                } else {
                                    addNullString();
                                }
                            }

                            public void jsonColumn(Column column) {
                                addDelimiter(column);
                                if (!pageReader.isNull(column)) {
                                    Value value = pageReader.getJson(column);
                                    addValue(value.toJson());
                                } else {
                                    addNullString();
                                }
                            }

                            private void addDelimiter(Column column) {
                                if (column.getIndex() != 0) {
                                    encoder.addText(delimiterString);
                                }
                            }

                            private void addValue(String v) {
                                encoder.addText(setEscapeAndQuoteValue(v, delimiter, quotePolicy, quote, escape, newlineInField, nullString));
                            }

                            private void addNullString() {
                                encoder.addText(nullString);
                            }

                            @SuppressWarnings("deprecation")  // For the use of org.embulk.spi.time.Timestamp.
                            private String formatTimestamp(final Column column) {
                                try {
                                    final Instant value = pageReader.getTimestampInstant(column);
                                    return timestampFormatters[column.getIndex()].format(value);
                                } catch (final NoSuchMethodError ex) {
                                    final org.embulk.spi.time.Timestamp value = pageReader.getTimestamp(column);
                                    return timestampFormatters[column.getIndex()].format(value.getInstant());
                                }
                            }
                        });
                    encoder.addNewLine();
                }
            }

            public void finish() {
                encoder.finish();
            }

            public void close() {
                encoder.close();
            }
        };
    }

    private void writeHeader(Schema schema, LineEncoder encoder, char delimiter, QuotePolicy policy, char quote, char escape, String newline, String nullString) {
        String delimiterString = String.valueOf(delimiter);
        for (Column column : schema.getColumns()) {
            if (column.getIndex() != 0) {
                encoder.addText(delimiterString);
            }
            encoder.addText(setEscapeAndQuoteValue(column.getName(), delimiter, policy, quote, escape, newline, nullString));
        }
        encoder.addNewLine();
    }

    private String setEscapeAndQuoteValue(String v, char delimiter, QuotePolicy policy, char quote, char escape, String newline, String nullString) {
        StringBuilder escapedValue = new StringBuilder();
        char previousChar = ' ';

        boolean isRequireQuote = (policy == QuotePolicy.ALL || policy == QuotePolicy.MINIMAL && v.equals(nullString)) ? true : false;

        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);

            if (policy != QuotePolicy.NONE && c == quote) {
                escapedValue.append(escape);
                escapedValue.append(c);
                isRequireQuote = true;
            } else if (c == '\r') {
                if (policy == QuotePolicy.NONE) {
                    escapedValue.append(escape);
                }
                escapedValue.append(newline);
                isRequireQuote = true;
            } else if (c == '\n') {
                if (previousChar != '\r') {
                    if (policy == QuotePolicy.NONE) {
                        escapedValue.append(escape);
                    }
                    escapedValue.append(newline);
                    isRequireQuote = true;
                }
            } else if (c == delimiter) {
                if (policy == QuotePolicy.NONE) {
                    escapedValue.append(escape);
                }
                escapedValue.append(c);
                isRequireQuote = true;
            } else {
                escapedValue.append(c);
            }
            previousChar = c;
        }

        if (policy != QuotePolicy.NONE && isRequireQuote) {
            return setQuoteValue(escapedValue.toString(), quote);
        } else {
            return escapedValue.toString();
        }
    }

    private String setQuoteValue(String v, char quote) {
        StringBuilder sb = new StringBuilder();
        sb.append(quote);
        sb.append(v);
        sb.append(quote);

        return sb.toString();
    }

    private static TimestampFormatter[] newTimestampColumnFormatters(
            final PluginTask task,
            final Schema schema,
            final Map<String, TimestampColumnOption> columnOptions) {
        final TimestampFormatter[] formatters = new TimestampFormatter[schema.getColumnCount()];
        int i = 0;
        for (final Column column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                final Optional<TimestampColumnOption> columnOption = Optional.ofNullable(columnOptions.get(column.getName()));

                final String pattern;
                if (columnOption.isPresent()) {
                    pattern = columnOption.get().getFormat().orElse(task.getDefaultTimestampFormat());
                } else {
                    pattern = task.getDefaultTimestampFormat();
                }

                final String zoneIdString;
                if (columnOption.isPresent()) {
                    zoneIdString = columnOption.get().getTimeZoneId().orElse(task.getDefaultTimeZoneId());
                } else {
                    zoneIdString = task.getDefaultTimeZoneId();
                }
                formatters[i] = TimestampFormatter.builder(pattern, true).setDefaultZoneFromString(zoneIdString).build();
            }
            i++;
        }
        return formatters;
    }

    @SuppressWarnings("deprecation")  // For the use of new PageReader().
    private static PageReader getPageReader(final Schema schema) {
        try {
            return Exec.getPageReader(schema);
        } catch (final NoSuchMethodError ex) {
            // Exec.getPageReader() is available from v0.10.17, and "new PageReader()" is deprecated then.
            // It is not expected to happen because this plugin is embedded with Embulk v0.10.24+, but falling back just in case.
            // TODO: Remove this fallback in v0.11.
            logger.warn("embulk-filter-remove_columns is expected to work with Embulk v0.10.17+.", ex);
            return new PageReader(schema);
        }
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder()
            .addDefaultModules()
            .addModule(new CharsetModule())
            .build();

    private static final Logger logger = LoggerFactory.getLogger(CsvFormatterPlugin.class);
}
