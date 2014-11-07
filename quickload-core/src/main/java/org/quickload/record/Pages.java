package org.quickload.record;

import java.util.List;
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
        PageReader reader = new PageReader(schema);
        for (Page page : pages) {
            try (RecordCursor cursor = reader.cursor(page)) {
                while (cursor.next()) {
                    final Object[] values = new Object[schema.getColumns().size()];
                    schema.consume(cursor, new RecordConsumer()
                    {
                        @Override
                        public void setNull(Column column)
                        {
                            // TODO
                        }

                        @Override
                        public void setLong(Column column, long value)
                        {
                            values[column.getIndex()] = value;
                        }

                        @Override
                        public void setDouble(Column column, double value)
                        {
                            values[column.getIndex()] = value;
                        }

                        @Override
                        public void setString(Column column, String value)
                        {
                            values[column.getIndex()] = value;
                        }
                    });
                    builder.add(values);
                }
            }
        }
        return builder.build();
    }
}
