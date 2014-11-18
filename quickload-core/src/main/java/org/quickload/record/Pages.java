package org.quickload.record;

import java.util.List;
import java.sql.Timestamp;
import com.google.common.collect.ImmutableList;

public class Pages
{
    public static List<Object[]> toObjects(Schema schema, Page page)
    {
        return toObjects(schema, ImmutableList.of(page));
    }

    public static List<Object[]> toObjects(Schema schema, Iterable<Page> pages)
    {
        ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
        try (PageReader reader = new PageReader(schema, pages)) {
            while (reader.nextRecord()) {
                final Object[] values = new Object[schema.getColumns().size()];
                reader.visitColumns(new RecordReader() {
                    @Override
                    public void readNull(Column column)
                    {
                        // TODO
                    }

                    @Override
                    public void readBoolean(Column column, boolean value)
                    {
                        values[column.getIndex()] = value;
                    }

                    @Override
                    public void readLong(Column column, long value)
                    {
                        values[column.getIndex()] = value;
                    }

                    @Override
                    public void readDouble(Column column, double value)
                    {
                        values[column.getIndex()] = value;
                    }

                    @Override
                    public void readString(Column column, String value)
                    {
                        values[column.getIndex()] = value;
                    }

                    @Override
                    public void readTimestamp(Column column, Timestamp value)
                    {
                        values[column.getIndex()] = value;
                    }
                });
                builder.add(values);
            }
        }
        return builder.build();
    }
}
