package org.embulk.standards;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigException;
import org.embulk.config.TaskSource;
import org.embulk.spi.type.TimestampType;
import org.embulk.spi.time.TimestampFormat;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.time.TimestampParseException;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnConfig;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfig;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInput;
import org.embulk.spi.PageOutput;
import org.embulk.spi.util.LineDecoder;
import org.slf4j.Logger;

public class CsvParserPlugin
        implements ParserPlugin
{
    private static final ImmutableSet<String> TRUE_STRINGS =
        ImmutableSet.of(
                "true", "True", "TRUE",
                "yes", "Yes", "YES",
                "t", "T", "y", "Y",
                "on", "On", "ON",
                "1");

    public interface PluginTask
            extends Task, LineDecoder.DecoderTask, TimestampParser.Task
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

        @Config("comment_line_marker")
        @ConfigDefault("null")
        public Optional<String> getCommentLineMarker();

        @Config("allow_optional_columns")
        @ConfigDefault("false")
        public boolean getAllowOptionalColumns();

        @Config("allow_extra_columns")
        @ConfigDefault("false")
        public boolean getAllowExtraColumns();
    }

    public interface TimestampColumnOption
            extends Task, TimestampParser.TimestampColumnOption
    { }

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

    private TimestampParser[] newTimestampParsers(
            TimestampParser.Task parserTask, SchemaConfig schema)
    {
        TimestampParser[] parsers = new TimestampParser[schema.getColumnCount()];
        int i = 0;
        for (ColumnConfig column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                TimestampColumnOption option = column.getOption().loadConfig(TimestampColumnOption.class);
                parsers[i] = new TimestampParser(parserTask, option);
            }
            i++;
        }
        return parsers;
    }

    @Override
    public void run(TaskSource taskSource, final Schema schema,
            FileInput input, PageOutput output)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        final TimestampParser[] timestampFormatters = newTimestampParsers(task, task.getSchemaConfig());
        LineDecoder lineDecoder = new LineDecoder(input, task);
        final CsvTokenizer tokenizer = new CsvTokenizer(lineDecoder, task);
        final String nullStringOrNull = task.getNullString().orNull();
        final boolean allowOptionalColumns = task.getAllowOptionalColumns();
        final boolean allowExtraColumns = task.getAllowExtraColumns();
        int skipHeaderLines = task.getSkipHeaderLines();

        try (final PageBuilder pageBuilder = new PageBuilder(Exec.getBufferAllocator(), schema, output)) {
            while (tokenizer.nextFile()) {
                // skip the header lines for each file
                for (; skipHeaderLines > 0; skipHeaderLines--) {
                    if (lineDecoder.poll() == null) {
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
                            public void booleanColumn(Column column)
                            {
                                String v = nextColumn();
                                if (v == null) {
                                    pageBuilder.setNull(column);
                                } else {
                                    pageBuilder.setBoolean(column, TRUE_STRINGS.contains(v));
                                }
                            }

                            public void longColumn(Column column)
                            {
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

                            public void doubleColumn(Column column)
                            {
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

                            public void stringColumn(Column column)
                            {
                                String v = nextColumn();
                                if (v == null) {
                                    pageBuilder.setNull(column);
                                } else {
                                    pageBuilder.setString(column, v);
                                }
                            }

                            public void timestampColumn(Column column)
                            {
                                String v = nextColumn();
                                if (v == null) {
                                    pageBuilder.setNull(column);
                                } else {
                                    try {
                                        pageBuilder.setTimestamp(column, timestampFormatters[column.getIndex()].parse(v));
                                    } catch (TimestampParseException e) {
                                        // TODO support default value
                                        throw new CsvRecordValidateException(e);
                                    }
                                }
                            }

                            private String nextColumn()
                            {
                                if (allowOptionalColumns && !tokenizer.hasNextColumn()) {
                                    //TODO warning
                                    return null;
                                }
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

                    } catch (CsvTokenizer.InvalidFormatException | CsvRecordValidateException e) {
                        long lineNumber = tokenizer.getCurrentLineNumber();
                        String skippedLine = tokenizer.skipCurrentLine();
                        log.warn(String.format("Skipped line %d (%s): %s", lineNumber, e.getMessage(), skippedLine));
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

    static class CsvRecordValidateException
            extends RuntimeException
    {
        CsvRecordValidateException(Throwable cause)
        {
            super(cause);
        }
    }
}
