package org.quickload.standards;

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

public class CsvParserPlugin extends BasicParserPlugin {
    @Override
    public TaskSource getBasicParserTask(ExecTask exec, ConfigSource config)
    {
        CsvParserTask task = exec.loadConfig(config, CsvParserTask.class);
        exec.setSchema(task.getSchemaConfig().toSchema());
        return exec.dumpTask(task);
    }

    private TimestampParser[] newTimestampParsers(final ExecTask exec, final TimestampParserTask parserTask, final Schema schema)
    {
        TimestampParser[] parsers = new TimestampParser[schema.getColumnCount()];
        for (Column column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                TimestampType tt = (TimestampType) column.getType();
                parsers[column.getIndex()] = exec.newTimestampParser(tt.getFormat(), parserTask);
            }
        }
        return parsers;
    }

    @Override
    public void runBasicParser(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, PageOutput pageOutput)
    {
        final PageAllocator pageAllocator = exec.getPageAllocator();
        final Schema schema = exec.getSchema();
        final CsvParserTask task = exec.loadTask(taskSource, CsvParserTask.class);
        final TimestampParser[] parsers = newTimestampParsers(exec, task, schema);
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
                                if (tokenizer.hasNextColumn()) {
                                    // if CsvTokenizer doesn't have any more columns, writer
                                    // is not called. just skipped.
                                    // TODO need any messages for skipping?
                                    String col = nextColumn(tokenizer);
                                    System.out.println("# writeBoolean: column: " + column.getName() + ", value: " + col);
                                    writer.write(Boolean.parseBoolean(nextColumn(tokenizer)));
                                }
                            }

                            public void writeLong(Column column, LongWriter writer) {
                                if (tokenizer.hasNextColumn()) {
                                    // if CsvTokenizer doesn't have any more columns, writer
                                    // is not called. just skipped.
                                    // TODO need any messages for skipping?
                                    String col = nextColumn(tokenizer);
                                    System.out.println("# writeLong: column: " + column.getName() + ", value: " + col);
                                    if (col == null) {
                                        writer.writeNull();
                                    } else {
                                        try {
                                            writer.write(Long.parseLong(col));
                                        } catch (NumberFormatException e) {
                                            throw e; // TODO
                                        }
                                    }
                                }
                            }

                            public void writeDouble(Column column, DoubleWriter writer) {
                                if (tokenizer.hasNextColumn()) {
                                    // if CsvTokenizer doesn't have any more columns, writer
                                    // is not called. just skipped.
                                    // TODO need any messages for skipping?
                                    String col = nextColumn(tokenizer);
                                    System.out.println("# writeDouble: column: " + column.getName() + ", value: " + col);
                                    if (col == null) {
                                        writer.writeNull();
                                    } else {
                                        try {
                                            writer.write(Double.parseDouble(col));
                                        } catch (NumberFormatException e) {
                                            throw e; // TODO
                                        }
                                    }
                                }
                            }

                            public void writeString(Column column, StringWriter writer) {
                                if (tokenizer.hasNextColumn()) {
                                    // if CsvTokenizer doesn't have any more columns, writer
                                    // is not called. just skipped.
                                    // TODO need any messages for skipping?
                                    String col = nextColumn(tokenizer);
                                    System.out.println("# writeString: column: " + column.getName() + ", value: " + col);
                                    if (col == null) {
                                        writer.writeNull();
                                    } else {
                                        writer.write(col);
                                    }
                                }
                            }

                            public void writeTimestamp(Column column, TimestampWriter writer) {
                                if (tokenizer.hasNextColumn()) {
                                    // if CsvTokenizer doesn't have any more columns, writer
                                    // is not called. just skipped.
                                    String col = nextColumn(tokenizer);
                                    System.out.println("# writeTimestamp: column: " + column.getName() + ", value: " + col);
                                    if (col == null) {
                                        writer.writeNull();
                                    } else {
                                        try {
                                            writer.write((parsers[column.getIndex()].parse(col)));
                                        } catch (Exception e) {
                                            // TODO
                                        }
                                    }
                                    // TODO timestamp parsing needs strptime format and default time zone
                                    //long msec = Long.parseLong(nextColumn(tokenizer));
                                    //Timestamp value = Timestamp.ofEpochMilli(msec);
                                    //writer.write(value);
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        exec.notice().skippedLine(tokenizer.getCurrentUntokenizedLine());
                        tokenizer.skipLine();
                    }
                }
            }
        }
    }

    private static String nextColumn(final CsvTokenizer tokenizer) // TODO user can select some mode
    {
        String column = tokenizer.nextColumn();
        // TODO users can select some mode for null handling
        if (!column.isEmpty()) {
            return column;
        } else {
            return tokenizer.isQuotedColumn() ? "" : null;
        }
    }
}
