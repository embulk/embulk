package org.quickload.spi;

import java.util.List;
import java.util.ArrayList;
import org.quickload.buffer.Buffer;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.PageOutput;
import org.quickload.record.Schema;
import org.quickload.record.PageBuilder;
import org.quickload.record.RecordProducer;
import org.quickload.record.Column;
import org.quickload.record.LongType;
import org.quickload.record.DoubleType;
import org.quickload.record.StringType;
import org.quickload.record.Record;

public class MockParserPlugin
        implements ParserPlugin
{
    private final Schema schema;
    private final Iterable<Record> records;
    private List<List<Buffer>> files;

    public MockParserPlugin(Schema schema, Iterable<Record> records)
    {
        this.schema = schema;
        this.records = records;
    }

    public List<List<Buffer>> getFiles()
    {
        return files;
    }

    public TaskSource getParserTask(ProcTask proc, ConfigSource config)
    {
        proc.setSchema(schema);
        return new TaskSource();
    }

    public void runParser(ProcTask proc,
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
        PageBuilder builder = new PageBuilder(proc.getPageAllocator(), schema, pageOutput);
        for (final Record record : records) {
            schema.produce(builder, new RecordProducer() {
                public void setLong(Column column, LongType.Setter setter)
                {
                    setter.setLong((long) record.getObject(column.getIndex()));
                }

                public void setDouble(Column column, DoubleType.Setter setter)
                {
                    setter.setDouble((double) record.getObject(column.getIndex()));
                }

                public void setString(Column column, StringType.Setter setter)
                {
                    setter.setString((String) record.getObject(column.getIndex()));
                }
            });
        }
        builder.flush();
    }
}
