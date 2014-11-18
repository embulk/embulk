package org.quickload.spi;

import java.util.List;
import java.util.ArrayList;
import java.sql.Timestamp;
import org.quickload.buffer.Buffer;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.config.Task;
import org.quickload.channel.PageInput;
import org.quickload.channel.FileBufferOutput;
import org.quickload.record.Schema;
import org.quickload.record.Page;
import org.quickload.record.PageReader;
import org.quickload.record.RecordReader;
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

        try (PageReader reader = new PageReader(schema, pageInput)) {
            while (reader.nextRecord()) {
                final Object[] values = new Object[schema.getColumns().size()];

                reader.visitColumns(new RecordReader() {
                    public void readNull(Column column)
                    {
                        values[column.getIndex()] = null;
                    }

                    public void readBoolean(Column column, boolean value)
                    {
                        values[column.getIndex()] = value;
                    }

                    public void readLong(Column column, long value)
                    {
                        values[column.getIndex()] = value;
                    }

                    public void readDouble(Column column, double value)
                    {
                        values[column.getIndex()] = value;
                    }

                    public void readString(Column column, String value)
                    {
                        values[column.getIndex()] = value;
                    }

                    public void readTimestamp(Column column, Timestamp value)
                    {
                        values[column.getIndex()] = value;
                    }
                });

                records.add(new Record(values));
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
