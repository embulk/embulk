package org.quickload.standards;

import org.quickload.time.Timestamp;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.PageOutput;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.record.Column;
import org.quickload.record.PageBuilder;
import org.quickload.record.RecordWriter;
import org.quickload.record.Schema;
import org.quickload.record.BooleanWriter;
import org.quickload.record.LongWriter;
import org.quickload.record.DoubleWriter;
import org.quickload.record.PageAllocator;
import org.quickload.record.StringWriter;
import org.quickload.record.TimestampWriter;
import org.quickload.spi.BasicParserPlugin;
import org.quickload.spi.LineDecoder;
import org.quickload.spi.ExecTask;

public class CsvParserPlugin extends BasicParserPlugin {
    @Override
    public TaskSource getBasicParserTask(ExecTask exec, ConfigSource config) {
        CsvParserTask task = exec.loadConfig(config, CsvParserTask.class);
        exec.setSchema(task.getSchemaConfig().toSchema());
        return exec.dumpTask(task);
    }

    @Override
    public void runBasicParser(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, PageOutput pageOutput) {
        final PageAllocator pageAllocator = exec.getPageAllocator();
        final Schema schema = exec.getSchema();
        final CsvParserTask task = exec.loadTask(taskSource, CsvParserTask.class);
        final CsvTokenizer tokenizer = new CsvTokenizer(new LineDecoder(fileBufferInput, task), task);
        try (PageBuilder builder = new PageBuilder(pageAllocator, schema, pageOutput)) {
            while (fileBufferInput.nextFile()) {
                boolean skipHeaderLine = task.getHeaderLine();
                while (tokenizer.hasNext()) {
                    if (skipHeaderLine) {
                        tokenizer.skipLine();
                        skipHeaderLine = false;
                        continue;
                    }

                    try {
                        builder.addRecord(new RecordWriter() {
                            public void writeBoolean(Column column, BooleanWriter writer) {
                                validateNextColumn(column, tokenizer);
                                writer.write(Boolean.parseBoolean(nextColumn(tokenizer)));
                            }

                            public void writeLong(Column column, LongWriter writer) {
                                validateNextColumn(column, tokenizer);
                                writer.write(Long.parseLong(nextColumn(tokenizer)));
                            }

                            public void writeDouble(Column column, DoubleWriter writer) {
                                validateNextColumn(column, tokenizer);
                                writer.write(Double.parseDouble(nextColumn(tokenizer)));
                            }

                            public void writeString(Column column, StringWriter writer) {
                                validateNextColumn(column, tokenizer);
                                writer.write(nextColumn(tokenizer));
                            }

                            public void writeTimestamp(Column column, TimestampWriter writer) {
                                validateNextColumn(column, tokenizer);
                                // TODO timestamp parsing needs strptime format and default time zone
                                long msec = Long.parseLong(nextColumn(tokenizer));
                                Timestamp value = Timestamp.ofEpochMilli(msec);
                                writer.write(value);
                            }
                        });
                    } catch (Exception e) {
                        exec.notice().skippedLine(tokenizer.getCurrentUntokenizedLine());
                        tokenizer.skipLine();
                    }
                }
            }
        }
    }

    private static void validateNextColumn(final Column column, final CsvTokenizer tokenizer)
    {
        if (!tokenizer.hasNextColumn()) {
            throw new RuntimeException("not much"); // TODO
        }
    }

    private static String nextColumn(final CsvTokenizer tokenizer)
    {
        String column = tokenizer.nextColumn();
        if (!column.isEmpty()) {
            return column;
        } else {
            return tokenizer.isQuotedColumn() ? "" : null;
        }
    }
}
