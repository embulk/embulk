package org.embulk.spi;

import java.util.List;
import com.google.common.collect.ImmutableList;
import org.embulk.time.Timestamp;
import org.embulk.type.Schema;
import org.embulk.type.Column;

public class Pages
{
    public static List<Object[]> toObjects(Schema schema, Page page)
    {
        return toObjects(schema, ImmutableList.of(page));
    }

    // TODO use streaming and return Iterable
    public static List<Object[]> toObjects(Schema schema, Iterable<Page> pages)
    {
        ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
        try (PageReader reader = new PageReader(schema, pages)) {
            while (reader.nextRecord()) {
                builder.add(toObjects(reader));
            }
        }
        return builder.build();
    }

    public static Object[] toObjects(PageReader record)
    {
        final Object[] values = new Object[record.getSchema().getColumns().size()];
        record.visitColumns(new RecordReader() {
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
        return values;
    }
}
