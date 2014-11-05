package org.quickload.spi;

import com.google.inject.Inject;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.record.Schema;
import org.quickload.record.RecordProducer;
import org.quickload.record.PageAllocator;
import org.quickload.record.PageBuilder;
import org.quickload.record.Column;
import org.quickload.record.LongType;
import org.quickload.record.DoubleType;
import org.quickload.record.StringType;
import org.quickload.channel.PageOutput;
import org.quickload.record.Record;

public class MockInputPlugin
        implements InputPlugin
{
    private final PageAllocator allocator;
    private final Schema schema;
    private final Iterable<Record> records;

    @Inject
    public MockInputPlugin(PageAllocator allocator,
            Schema schema, Iterable<Record> records)
    {
        this.allocator = allocator;
        this.schema = schema;
        this.records = records;
    }

    public NextConfig runInputTransaction(ProcTask proc, ConfigSource config,
            ProcControl control)
    {
        control.run(new TaskSource());
        return new NextConfig();
    }

    public Report runInput(ProcTask proc, TaskSource taskSource,
            int processorIndex, PageOutput pageOutput)
    {
        PageBuilder builder = new PageBuilder(allocator, schema, pageOutput);
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
        return new Report();
    }
}
