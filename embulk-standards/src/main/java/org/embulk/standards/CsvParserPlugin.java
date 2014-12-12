package org.embulk.standards;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.embulk.channel.FileBufferInput;
import org.embulk.channel.PageOutput;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.record.BooleanWriter;
import org.embulk.record.Column;
import org.embulk.record.DoubleWriter;
import org.embulk.record.LongWriter;
import org.embulk.record.PageAllocator;
import org.embulk.record.PageBuilder;
import org.embulk.record.RecordWriter;
import org.embulk.record.Schema;
import org.embulk.record.StringWriter;
import org.embulk.record.TimestampType;
import org.embulk.record.TimestampWriter;
import org.embulk.spi.BasicParserPlugin;
import org.embulk.spi.LineDecoder;
import org.embulk.spi.ExecTask;
import org.embulk.time.TimestampParseException;
import org.embulk.time.TimestampParser;
import org.embulk.time.TimestampParserTask;

import java.util.Map;

public class CsvParserPlugin
        extends BasicParserPlugin
{
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
                while (tokenizer.nextRecord()) {
                    if (skipHeaderLine) {
                        tokenizer.skipCurrentLine();
                        skipHeaderLine = false;
                        continue;
                    }

                    try {
                        builder.addRecord(new RecordWriter() {
                            public void writeBoolean(Column column, BooleanWriter writer) {
                                String v = nextColumn(task, tokenizer);
                                if (v != null) {
                                    writer.write(Boolean.parseBoolean(v));
                                } else {
                                    writer.writeNull();
                                }
                            }

                            public void writeLong(Column column, LongWriter writer) {
                                String v = nextColumn(task, tokenizer);
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
                                String v = nextColumn(task, tokenizer);
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
                                String v = nextColumn(task, tokenizer);
                                if (v != null) {
                                    writer.write(v);
                                } else {
                                    writer.writeNull();
                                }
                            }

                            public void writeTimestamp(Column column, TimestampWriter writer) {
                                String v = nextColumn(task, tokenizer);
                                if (v == null) {
                                    writer.writeNull();
                                    return;
                                }

                                try {
                                    writer.write((tsParsers.get(column.getIndex()).parse(v)));
                                } catch (TimestampParseException e) {
                                    throw new CsvRecordValidateException(e);
                                }
                            }
                        });
                    } catch (Exception e) {
                        exec.notice().skippedLine(tokenizer.skipCurrentLine());
                    }
                }
            }
        }
    }

    private static String nextColumn(final CsvParserTask task, final CsvTokenizer tokenizer)
    {
        String v = Preconditions.checkNotNull(tokenizer.nextColumn(), "should not be null");
        if (!v.isEmpty()) {
            return v;
        }

        if (tokenizer.wasQuotedColumn()) {
            return "";
        }

        if (task.getNullString().isPresent()) {
            return task.getNullString().get();
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
