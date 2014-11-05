package org.quickload.spi;

import java.util.List;
import java.util.ArrayList;
import org.quickload.buffer.Buffer;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.config.Task;
import org.quickload.channel.PageInput;
import org.quickload.channel.FileBufferOutput;
import org.quickload.record.Schema;
import org.quickload.record.RecordConsumer;
import org.quickload.record.Page;
import org.quickload.record.PageReader;
import org.quickload.record.RecordCursor;
import org.quickload.record.Column;
import org.quickload.record.Record;

public class MockFormatterPlugin
        implements FormatterPlugin
{
    private final Iterable<? extends Iterable<Buffer>> files;
    private final Class<? extends Task> taskIface;
    private Schema schema;
    private List<Record> records;

    public <F extends Iterable<Buffer>> MockFormatterPlugin(Iterable<F> files)
    {
        this(files, Task.class);
    }

    public <F extends Iterable<Buffer>, T extends Task> MockFormatterPlugin(Iterable<F> files, Class<T> taskIface)
    {
        this.files = files;
        this.taskIface = taskIface;
    }

    public Iterable<? extends Iterable<Buffer>> getFiles()
    {
        return files;
    }

    public Schema getSchema()
    {
        return schema;
    }

    public List<Record> getRecords()
    {
        return records;
    }

    public TaskSource getFormatterTask(ProcTask proc, ConfigSource config)
    {
        return proc.dumpTask(proc.loadConfig(config, taskIface));
    }

    public void runFormatter(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            PageInput pageInput, FileBufferOutput fileBufferOutput)
    {
        records = new ArrayList<Record>();
        schema = proc.getSchema();

        PageReader reader = new PageReader(schema);
        for (Page page : pageInput) {
            try (RecordCursor cursor = reader.cursor(page)) {
                while (cursor.next()) {
                    final Object[] values = new Object[schema.getColumnCount()];

                    schema.consume(cursor, new RecordConsumer() {
                        public void setNull(Column column)
                        {
                            values[column.getIndex()] = null;
                        }

                        public void setLong(Column column, long value)
                        {
                            values[column.getIndex()] = (Long) value;
                        }

                        public void setDouble(Column column, double value)
                        {
                            values[column.getIndex()] = (Double) value;
                        }

                        public void setString(Column column, String value)
                        {
                            values[column.getIndex()] = value;
                        }
                    });

                    records.add(new Record(values));
                }
            }
        }

        for (Iterable<Buffer> buffers : files) {
            for (Buffer buffer : buffers) {
                fileBufferOutput.add(buffer);
            }
            fileBufferOutput.addFile();
        }
    }
}
