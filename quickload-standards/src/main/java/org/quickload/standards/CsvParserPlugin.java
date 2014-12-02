package org.quickload.standards;

import com.google.common.collect.ImmutableMap;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.PageOutput;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.record.BooleanWriter;
import org.quickload.record.Column;
import org.quickload.record.DoubleWriter;
import org.quickload.record.LongWriter;
import org.quickload.record.PageAllocator;
import org.quickload.record.PageBuilder;
import org.quickload.record.RecordWriter;
import org.quickload.record.Schema;
import org.quickload.record.StringWriter;
import org.quickload.record.TimestampType;
import org.quickload.record.TimestampWriter;
import org.quickload.spi.BasicParserPlugin;
import org.quickload.spi.LineDecoder;
import org.quickload.spi.ExecTask;
import org.quickload.time.TimestampParser;
import org.quickload.time.TimestampParserTask;

import java.util.Map;

public class CsvParserPlugin
        extends BasicParserPlugin {
    @Override
    public TaskSource getBasicParserTask(ExecTask exec, ConfigSource config)
    {
        CsvParserTask task = exec.loadConfig(config, CsvParserTask.class);
        exec.setSchema(task.getSchemaConfig().toSchema());
        return exec.dumpTask(task);
    }

    private Map<Integer, TimestampParser> newTimestampParsers(
            final ExecTask exec, final TimestampParserTask parserTask,
            final Schema schema)
    {
        ImmutableMap.Builder<Integer, TimestampParser> builder = new ImmutableMap.Builder<>();
        for (Column column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                TimestampType tt = (TimestampType) column.getType();
                builder.put(column.getIndex(), exec.newTimestampParser(tt.getFormat(), parserTask));
            }
        }
        return builder.build();
    }

    @Override
    public void runBasicParser(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, PageOutput pageOutput)
    {
        final PageAllocator pageAllocator = exec.getPageAllocator();
        final Schema schema = exec.getSchema();
        final CsvParserTask task = exec.loadTask(taskSource, CsvParserTask.class);
        final Map<Integer, TimestampParser> tsParsers = newTimestampParsers(exec, task, schema);
        final CsvTokenizer tokenizer = new CsvTokenizer(new LineDecoder(fileBufferInput, task), task);
        try (PageBuilder builder = new PageBuilder(pageAllocator, schema, pageOutput)) {
            while (fileBufferInput.nextFile()) {
                boolean skipHeaderLine = task.getHeaderLine();
                while (tokenizer.hasNextRecord()) {
                    if (skipHeaderLine) {
                        tokenizer.skipLine();
                        skipHeaderLine = false;
                        continue;
                    }

                    try {
                        builder.addRecord(new RecordWriter() {
                            public void writeBoolean(Column column, BooleanWriter writer) {
                                String v = nextColumn(tokenizer);
                                if (v != null) {
                                    writer.write(Boolean.parseBoolean(v));
                                } else {
                                    writer.writeNull();
                                }
                            }

                            public void writeLong(Column column, LongWriter writer) {
                                String v = nextColumn(tokenizer);
                                if (v == null) {
                                    writer.writeNull();
                                    return;
                                }

                                try {
                                    writer.write(Long.parseLong(v));
                                } catch (NumberFormatException e) {
                                    throw new CsvRecordValidateException(e);
                                }
                            }

                            public void writeDouble(Column column, DoubleWriter writer) {
                                String v = nextColumn(tokenizer);
                                if (v == null) {
                                    writer.writeNull();
                                    return;
                                }

                                try {
                                    writer.write(Double.parseDouble(v));
                                } catch (NumberFormatException e) {
                                    throw new CsvRecordValidateException(e);
                                }
                            }

                            public void writeString(Column column, StringWriter writer) {
                                String v = nextColumn(tokenizer);
                                if (v != null) {
                                    writer.write(v);
                                } else {
                                    writer.writeNull();
                                }
                            }

                            public void writeTimestamp(Column column, TimestampWriter writer) {
                                String v = nextColumn(tokenizer);
                                if (v == null) {
                                    writer.writeNull();
                                    return;
                                }

                                try {
                                    writer.write((tsParsers.get(column.getIndex()).parse(v)));
                                } catch (Exception e) {
                                    throw new CsvRecordValidateException(e);
                                }
                            }
                        });

                        tokenizer.nextRecord();
                    } catch (Exception e) {
                        exec.notice().skippedLine(tokenizer.getCurrentUntokenizedLine());
                        tokenizer.skipLine();
                    }
                }
            }
        }
    }

    private static String nextColumn(final CsvTokenizer tokenizer)
    {
        String v = tokenizer.nextColumn();
        if (v.isEmpty()) {
            return tokenizer.isQuotedColumn() ? "" : null;
        } else {
            return v;
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
