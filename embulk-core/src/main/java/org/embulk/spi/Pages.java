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
        record.getSchema().visitColumns(new ObjectSchemaVisitor(record) {
            @Override
            public void visit(Column column, Object object)
            {
                values[column.getIndex()] = object;
            }
        });
        return values;
    }

    public static abstract class ObjectSchemaVisitor
            implements SchemaVisitor
    {
        private final PageReader record;

        public ObjectSchemaVisitor(PageReader record)
        {
            this.record = record;
        }

        public abstract void visit(Column column, Object obj);

        @Override
        public void booleanColumn(Column column)
        {
            if (record.isNull(column)) {
                visit(column, null);
            } else {
                visit(column, record.getBoolean(column));
            }
        }

        @Override
        public void longColumn(Column column)
        {
            if (record.isNull(column)) {
                visit(column, null);
            } else {
                visit(column, record.getLong(column));
            }
        }

        @Override
        public void doubleColumn(Column column)
        {
            if (record.isNull(column)) {
                visit(column, null);
            } else {
                visit(column, record.getDouble(column));
            }
        }

        @Override
        public void stringColumn(Column column)
        {
            if (record.isNull(column)) {
                visit(column, null);
            } else {
                visit(column, record.getString(column));
            }
        }

        @Override
        public void timestampColumn(Column column)
        {
            if (record.isNull(column)) {
                visit(column, null);
            } else {
                visit(column, record.getTimestamp(column));
            }
        }
    }

    public static Object getObject(PageReader record, Column column)
    {
        GetObjectColumnVisitor visitor = new GetObjectColumnVisitor(record);
        column.visit(visitor);
        return visitor.get();
    }

    private static class GetObjectColumnVisitor
            extends ObjectSchemaVisitor
    {
        private Object object;

        public GetObjectColumnVisitor(PageReader record)
        {
            super(record);
        }

        public Object get()
        {
            return object;
        }

        public void visit(Column column, Object object)
        {
            this.object = object;
        }
    }
}
