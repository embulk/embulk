package org.embulk.spi;

import java.util.List;
import java.util.Iterator;
import com.google.common.collect.ImmutableList;
import org.embulk.time.Timestamp;
import org.embulk.type.Schema;
import org.embulk.type.SchemaVisitor;
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
        Iterator<Page> ite = pages.iterator();
        try (PageReader reader = new PageReader(schema)) {
            while (ite.hasNext()) {
                reader.setPage(ite.next());
                while (reader.nextRecord()) {
                    builder.add(toObjects(reader));
                }
            }
        }
        return builder.build();
    }

    public static Object[] toObjects(final PageReader record)
    {
        final Object[] values = new Object[record.getSchema().getColumns().size()];
        record.getSchema().visitColumns(new SchemaVisitor() {
            @Override
            public void booleanColumn(Column column)
            {
                if (record.isNull(column.getIndex())) {
                    values[column.getIndex()] = null;
                } else {
                    values[column.getIndex()] = record.getBoolean(column.getIndex());
                }
            }

            @Override
            public void longColumn(Column column)
            {
                if (record.isNull(column.getIndex())) {
                    values[column.getIndex()] = null;
                } else {
                    values[column.getIndex()] = record.getLong(column.getIndex());
                }
            }

            @Override
            public void doubleColumn(Column column)
            {
                if (record.isNull(column.getIndex())) {
                    values[column.getIndex()] = null;
                } else {
                    values[column.getIndex()] = record.getDouble(column.getIndex());
                }
            }

            @Override
            public void stringColumn(Column column)
            {
                if (record.isNull(column.getIndex())) {
                    values[column.getIndex()] = null;
                } else {
                    values[column.getIndex()] = record.getString(column.getIndex());
                }
            }

            @Override
            public void timestampColumn(Column column)
            {
                if (record.isNull(column.getIndex())) {
                    values[column.getIndex()] = null;
                } else {
                    values[column.getIndex()] = record.getTimestamp(column.getIndex());
                }
            }
        });
        return values;
    }
}
