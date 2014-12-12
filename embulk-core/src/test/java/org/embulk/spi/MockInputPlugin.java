package org.embulk.spi;

import org.embulk.channel.PageOutput;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
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
