package org.quickload.spi;

import org.quickload.channel.PageOutput;
import org.quickload.config.ConfigSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
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

public class MockInputPlugin
        implements InputPlugin
{
    private final Schema schema;
    private final Iterable<Record> records;

    public MockInputPlugin(Schema schema, Iterable<Record> records)
    {
        this.schema = schema;
        this.records = records;
    }

    public NextConfig runInputTransaction(ExecTask exec, ConfigSource config,
            ExecControl control)
    {
        control.run(new TaskSource());
        return new NextConfig();
    }

    public Report runInput(ExecTask exec, TaskSource taskSource,
            int processorIndex, PageOutput pageOutput)
    {
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
        return new Report();
    }
}
