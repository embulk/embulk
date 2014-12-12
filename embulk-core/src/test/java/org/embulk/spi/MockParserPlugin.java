package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;

import org.embulk.buffer.Buffer;
import org.embulk.channel.FileBufferInput;
import org.embulk.channel.PageOutput;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.record.BooleanWriter;
import org.embulk.record.Column;
import org.embulk.record.DoubleWriter;
import org.embulk.record.LongWriter;
import org.embulk.record.PageBuilder;
import org.embulk.record.Record;
import org.embulk.record.RecordWriter;
import org.embulk.record.Schema;
import org.embulk.record.StringWriter;
import org.embulk.record.TimestampWriter;
import org.embulk.time.Timestamp;

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
