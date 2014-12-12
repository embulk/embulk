package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;

import org.embulk.buffer.Buffer;
import org.embulk.channel.FileBufferOutput;
import org.embulk.channel.PageInput;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.record.Column;
import org.embulk.record.PageReader;
import org.embulk.record.Record;
import org.embulk.record.RecordReader;
import org.embulk.record.Schema;
import org.embulk.time.Timestamp;

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

    public TaskSource getFormatterTask(ExecTask exec, ConfigSource config)
    {
        return exec.dumpTask(exec.loadConfig(config, taskIface));
    }

    public void runFormatter(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            PageInput pageInput, FileBufferOutput fileBufferOutput)
    {
        records = new ArrayList<Record>();
        schema = exec.getSchema();

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
