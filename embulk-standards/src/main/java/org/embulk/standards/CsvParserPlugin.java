package org.embulk.standards;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.base.Optional;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
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
        ImmutableSet.<String>of(
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

        @Config("header_line") // how to set default value?? TODO @Default("true")
        @ConfigDefault("false")
        public boolean getHeaderLine();

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
        final CsvTokenizer tokenizer = new CsvTokenizer(new LineDecoder(input, task), task);
        final String nullStringOrNull = task.getNullString().orNull();
        boolean skipHeaderLine = task.getHeaderLine();

        try (final PageBuilder pageBuilder = new PageBuilder(Exec.getBufferAllocator(), schema, output)) {
            while (tokenizer.nextFile()) {
                if (skipHeaderLine) {
                    // skip the first line
                    if (tokenizer.nextRecord()) {
                        for (int i=0; i < schema.getColumnCount(); i++) {
                            tokenizer.nextColumn();  // TODO check return value?
                        }
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
