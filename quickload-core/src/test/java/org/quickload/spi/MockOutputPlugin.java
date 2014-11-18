package org.quickload.spi;

import java.util.List;
import java.util.ArrayList;
import java.sql.Timestamp;
import com.google.inject.Inject;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.channel.PageInput;
import org.quickload.record.Schema;
import org.quickload.record.Page;
import org.quickload.record.PageReader;
import org.quickload.record.RecordReader;
import org.quickload.record.Column;
import org.quickload.record.Record;

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

    public NextConfig runOutputTransaction(ProcTask proc, ConfigSource config,
            ProcControl control)
    {
        control.run(new TaskSource());
        return new NextConfig();
    }

    public Report runOutput(ProcTask proc, TaskSource taskSource,
            int processorIndex, PageInput pageInput)
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
        return new Report();
    }
}
