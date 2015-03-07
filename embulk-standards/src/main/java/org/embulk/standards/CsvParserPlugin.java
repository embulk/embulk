package org.embulk.standards;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigException;
import org.embulk.config.TaskSource;
import org.embulk.spi.type.TimestampType;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.time.TimestampParseException;
import org.embulk.spi.Column;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfig;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInput;
import org.embulk.spi.PageOutput;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.util.LineDecoder;
import org.slf4j.Logger;

import java.util.Map;

public class CsvParserPlugin
        implements ParserPlugin
{
    private static final ImmutableSet<String> TRUE_STRINGS =
        ImmutableSet.of(
                "true", "True", "TRUE",
                "yes", "Yes", "YES",
                "y", "Y",
                "on", "On", "ON",
                "1");

    public interface PluginTask
            extends Task, LineDecoder.DecoderTask, TimestampParser.ParserTask
    {
        @Config("columns")
        public SchemaConfig getSchemaConfig();

        @Config("header_line")
        @ConfigDefault("null")
        public Optional<Boolean> getHeaderLine();

        @Config("skip_header_lines")
        @ConfigDefault("0")
        public int getSkipHeaderLines();
        public void setSkipHeaderLines(int n);

        @Config("delimiter")
        @ConfigDefault("\",\"")
        public char getDelimiterChar();

        @Config("quote")
        @ConfigDefault("\"\\\"\"")
        public char getQuoteChar();

        @Config("escape")
        @ConfigDefault("\"\\\\\"")
        public char getEscapeChar();

        // Null value handling: if the CsvParser found 'non-quoted empty string's,
        // it replaces them to string that users specified like "\N", "NULL".
        @Config("null_string")
        @ConfigDefault("null")
        public Optional<String> getNullString();

        @Config("trim_if_not_quoted")
        @ConfigDefault("false")
        public boolean getTrimIfNotQuoted();

        @Config("max_quoted_size_limit")
        @ConfigDefault("131072") //128kB
        public long getMaxQuotedSizeLimit();
    }

    private final Logger log;

    public CsvParserPlugin()
    {
        log = Exec.getLogger(CsvParserPlugin.class);
    }

    @Override
    public void transaction(ConfigSource config, ParserPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);

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

    private Map<Integer, TimestampParser> newTimestampParsers(
            TimestampParser.ParserTask task, Schema schema)
    {
        ImmutableMap.Builder<Integer, TimestampParser> builder = new ImmutableMap.Builder<>();
        for (Column column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                TimestampType tt = (TimestampType) column.getType();
                builder.put(column.getIndex(), new TimestampParser(tt.getFormat(), task));
            }
        }
        return builder.build();
    }

    @Override
    public void run(TaskSource taskSource, final Schema schema,
            FileInput input, PageOutput output)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        final Map<Integer, TimestampParser> timestampFormatters = newTimestampParsers(task, schema);
        LineDecoder lineDecoder = new LineDecoder(input, task);
        final CsvTokenizer tokenizer = new CsvTokenizer(lineDecoder, task);
        final String nullStringOrNull = task.getNullString().orNull();
        int skipHeaderLines = task.getSkipHeaderLines();

        try (final PageBuilder pageBuilder = new PageBuilder(Exec.getBufferAllocator(), schema, output)) {
            while (tokenizer.nextFile()) {
                // skip the header lines for each file
                for (; skipHeaderLines > 0; skipHeaderLines--) {
                    if (lineDecoder.poll() == null) {
                        break;
                    }
                }

                while (true) {
                    try {
                        if (!tokenizer.nextRecord()) {
                            break;
                        }

                        schema.visitColumns(new ColumnVisitor() {
                            public void booleanColumn(Column column)
                            {
                                String v = nextColumn(schema, tokenizer, nullStringOrNull);
                                if (v == null) {
                                    pageBuilder.setNull(column);
                                } else {
                                    pageBuilder.setBoolean(column, TRUE_STRINGS.contains(v));
                                }
                            }

                            public void longColumn(Column column)
                            {
                                String v = nextColumn(schema, tokenizer, nullStringOrNull);
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

                            public void doubleColumn(Column column)
                            {
                                String v = nextColumn(schema, tokenizer, nullStringOrNull);
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

                            public void stringColumn(Column column)
                            {
                                String v = nextColumn(schema, tokenizer, nullStringOrNull);
                                if (v == null) {
                                    pageBuilder.setNull(column);
                                } else {
                                    pageBuilder.setString(column, v);
                                }
                            }

                            public void timestampColumn(Column column)
                            {
                                String v = nextColumn(schema, tokenizer, nullStringOrNull);
                                if (v == null) {
                                    pageBuilder.setNull(column);
                                } else {
                                    try {
                                        pageBuilder.setTimestamp(column, (timestampFormatters.get(column.getIndex()).parse(v)));
                                    } catch (TimestampParseException e) {
                                        // TODO support default value
                                        throw new CsvRecordValidateException(e);
                                    }
                                }
                            }
                        });
                        pageBuilder.addRecord();

                    } catch (Exception e) {
                        // TODO logging
                        long lineNumber = tokenizer.getCurrentLineNumber();
                        String skippedLine = tokenizer.skipCurrentLine();
                        log.warn(String.format("Skipped (line %d): %s", lineNumber, skippedLine), e);
                        //exec.notice().skippedLine(skippedLine);
                    }
                }
            }

            pageBuilder.finish();
        }
    }

    private static String nextColumn(Schema schema, CsvTokenizer tokenizer, String nullStringOrNull)
    {
        String v = tokenizer.nextColumn();
        if (!v.isEmpty()) {
            if (v.equals(nullStringOrNull)) {
                return null;
            }
            return v;
        } else if (tokenizer.wasQuotedColumn()) {
            return "";
        } else {
            return null;
        }
    }

    static class CsvRecordValidateException
            extends RuntimeException
    {
        CsvRecordValidateException(Throwable cause)
        {
            super(cause);
        }
    }
}
