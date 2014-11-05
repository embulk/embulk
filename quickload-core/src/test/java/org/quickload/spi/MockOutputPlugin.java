package org.quickload.spi;

import java.util.List;
import java.util.ArrayList;
import com.google.inject.Inject;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.record.Schema;
import org.quickload.record.RecordConsumer;
import org.quickload.record.Page;
import org.quickload.record.PageReader;
import org.quickload.record.RecordCursor;
import org.quickload.record.Column;
import org.quickload.record.LongType;
import org.quickload.record.DoubleType;
import org.quickload.record.StringType;
import org.quickload.channel.PageInput;
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

        PageReader reader = new PageReader(schema);
        for (Page page : pageInput) {
            try (RecordCursor cursor = reader.cursor(page)) {
                while (cursor.next()) {
                    final Object[] values = new Object[schema.getColumnCount()];

                    schema.consume(cursor, new RecordConsumer() {
                        public void setNull(Column column)
                        {
                            values[column.getIndex()] = null;
                        }

                        public void setLong(Column column, long value)
                        {
                            values[column.getIndex()] = (Long) value;
                        }

                        public void setDouble(Column column, double value)
                        {
                            values[column.getIndex()] = (Double) value;
                        }

                        public void setString(Column column, String value)
                        {
                            values[column.getIndex()] = value;
                        }
                    });

                    records.add(new Record(values));
                }
            }
        }
        return new Report();
    }
}
