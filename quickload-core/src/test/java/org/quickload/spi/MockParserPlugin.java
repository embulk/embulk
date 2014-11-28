package org.quickload.spi;

import java.util.ArrayList;
import java.util.List;

import org.quickload.buffer.Buffer;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.PageOutput;
import org.quickload.config.ConfigSource;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.record.BooleanWriter;
import org.quickload.record.Column;
import org.quickload.record.DoubleWriter;
import org.quickload.record.LongWriter;
import org.quickload.record.PageBuilder;
import org.quickload.record.Record;
import org.quickload.record.RecordWriter;
import org.quickload.record.Schema;
import org.quickload.record.StringWriter;
import org.quickload.record.TimestampWriter;
import org.quickload.time.Timestamp;

public class MockParserPlugin
        implements ParserPlugin
{
    private final Schema schema;
    private final Iterable<Record> records;
    private final Class<? extends Task> taskIface;
    private List<List<Buffer>> files;

    public MockParserPlugin(Schema schema, Iterable<Record> records)
    {
        this(schema, records, Task.class);
    }

    public MockParserPlugin(Schema schema, Iterable<Record> records, Class<? extends Task> taskIface)
    {
        this.schema = schema;
        this.records = records;
        this.taskIface = taskIface;
    }

    public Schema getSchema()
    {
        return schema;
    }

    public Iterable<Record> getRecords()
    {
        return records;
    }

    public List<List<Buffer>> getFiles()
    {
        return files;
    }

    public TaskSource getParserTask(ExecTask exec, ConfigSource config)
    {
        exec.setSchema(schema);
        return exec.dumpTask(exec.loadConfig(config, taskIface));
    }

    public void runParser(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, PageOutput pageOutput)
    {
        files = new ArrayList<List<Buffer>>();
        while (fileBufferInput.nextFile()) {
            List<Buffer> buffers = new ArrayList<Buffer>();
            for (Buffer buffer : fileBufferInput) {
                buffers.add(buffer);
            }
            files.add(buffers);
        }
        try (PageBuilder builder = new PageBuilder(exec.getPageAllocator(), schema, pageOutput)) {
            for (final Record record : records) {
                builder.addRecord(new RecordWriter() {
                    public void writeBoolean(Column column, BooleanWriter writer)
                    {
                        writer.write((boolean) record.getObject(column.getIndex()));
                    }

                    public void writeLong(Column column, LongWriter writer)
                    {
                        writer.write((long) record.getObject(column.getIndex()));
                    }

                    public void writeDouble(Column column, DoubleWriter writer)
                    {
                        writer.write((double) record.getObject(column.getIndex()));
                    }

                    public void writeString(Column column, StringWriter writer)
                    {
                        writer.write((String) record.getObject(column.getIndex()));
                    }

                    public void writeTimestamp(Column column, TimestampWriter writer)
                    {
                        writer.write((Timestamp) record.getObject(column.getIndex()));
                    }
                });
            }
        }
    }
}
