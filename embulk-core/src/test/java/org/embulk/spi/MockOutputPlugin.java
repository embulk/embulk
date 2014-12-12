package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;

import org.embulk.channel.PageInput;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
import org.embulk.config.TaskSource;
import org.embulk.record.Column;
import org.embulk.record.PageReader;
import org.embulk.record.Record;
import org.embulk.record.RecordReader;
import org.embulk.record.Schema;
import org.embulk.time.Timestamp;

public class MockOutputPlugin
        implements OutputPlugin
{
    private Schema schema;
    private List<Record> records;

    public Schema getSchema()
    {
        return schema;
    }

    public List<Record> getRecords()
    {
        return records;
    }

    public NextConfig runOutputTransaction(ExecTask exec, ConfigSource config,
            ExecControl control)
    {
        control.run(new TaskSource());
        return new NextConfig();
    }

    public Report runOutput(ExecTask exec, TaskSource taskSource,
            int processorIndex, PageInput pageInput)
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
        return new Report();
    }
}
