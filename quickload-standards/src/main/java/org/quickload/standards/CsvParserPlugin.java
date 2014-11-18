package org.quickload.standards;

import java.sql.Timestamp;
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
import org.quickload.record.StringWriter;
import org.quickload.record.TimestampWriter;
import org.quickload.record.StringWriter;
import org.quickload.spi.BasicParserPlugin;
import org.quickload.spi.LineDecoder;
import org.quickload.spi.ProcTask;

import java.util.List;

public class CsvParserPlugin extends BasicParserPlugin {
    @Override
    public TaskSource getBasicParserTask(ProcTask proc, ConfigSource config) {
        CsvParserTask task = proc.loadConfig(config, CsvParserTask.class);
        proc.setSchema(task.getSchemaConfig().toSchema());
        return proc.dumpTask(task);
    }

    @Override
    public void runBasicParser(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, PageOutput pageOutput) {
        Schema schema = proc.getSchema();
        CsvParserTask task = proc.loadTask(taskSource, CsvParserTask.class);
        CsvTokenizer tokenizer = new CsvTokenizer(new LineDecoder(fileBufferInput, task), task);
        try (PageBuilder builder = new PageBuilder(proc.getPageAllocator(), proc.getSchema(), pageOutput)) {
            while (fileBufferInput.nextFile()) {
                boolean skipHeaderLine = task.getHeaderLine();
                for (final List<String> record : tokenizer) {
                    if (record.size() != schema.getColumns().size()) {
                        throw new RuntimeException("not match"); // TODO fix the error handling
                    }

                    if (skipHeaderLine) {
                        skipHeaderLine = false;
                        continue;
                    }

                    builder.addRecord(new RecordWriter() {
                        // TODO null column string
                        public void writeBoolean(Column column, BooleanWriter writer)
                        {
                            writer.write(Boolean.parseBoolean(record.get(column.getIndex())));
                        }

                        public void writeLong(Column column, LongWriter writer)
                        {
                            writer.write(Long.parseLong(record.get(column.getIndex())));
                        }

                        public void writeDouble(Column column, DoubleWriter writer)
                        {
                            writer.write(Double.parseDouble(record.get(column.getIndex())));
                        }

                        public void writeString(Column column, StringWriter writer)
                        {
                            writer.write(record.get(column.getIndex()));
                        }

                        public void writeTimestamp(Column column, TimestampWriter writer)
                        {
                            // TODO timestamp parsing needs strptime format and default time zone
                            long msec = Long.parseLong(record.get(column.getIndex()));
                            int nsec = 0;
                            Timestamp value = new Timestamp(msec);
                            value.setNanos(nsec);
                            writer.write(value);
                        }
                    });
                }
            }
        }
    }
}
