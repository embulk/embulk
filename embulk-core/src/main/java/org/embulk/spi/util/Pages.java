package org.embulk.spi.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.Page;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;

/**
 * A utility class to manupulate {@link org.embulk.spi.Page}s.
 *
 * @deprecated Use {@link org.embulk.spi.PageReader} directly.
 */
@Deprecated
public class Pages {
    public static List<Object[]> toObjects(Schema schema, Page page) {
        final ArrayList<Page> pages = new ArrayList<>();
        pages.add(page);
        return toObjects(schema, Collections.unmodifiableList(pages));
    }

    // TODO use streaming and return Iterable
    public static List<Object[]> toObjects(final Schema schema, final Iterable<Page> pages, final boolean useInstant) {
        final ArrayList<Object[]> builder = new ArrayList<>();
        Iterator<Page> ite = pages.iterator();
        try (PageReader reader = new PageReader(schema)) {
            while (ite.hasNext()) {
                reader.setPage(ite.next());
                while (reader.nextRecord()) {
                    builder.add(toObjects(reader, useInstant));
                }
            }
        }
        return Collections.unmodifiableList(builder);
    }

    public static List<Object[]> toObjects(Schema schema, Iterable<Page> pages) {
        return toObjects(schema, pages, false);
    }

    public static Object[] toObjects(final PageReader record, final boolean useInstant) {
        final Object[] values = new Object[record.getSchema().getColumns().size()];
        record.getSchema().visitColumns(new ObjectColumnVisitor(record, useInstant) {
                @Override
                public void visit(Column column, Object object) {
                    values[column.getIndex()] = object;
                }
            });
        return values;
    }

    public static Object[] toObjects(final PageReader record) {
        return toObjects(record, false);
    }

    /**
     * A {@link ColumnVisitor} implementation to map everything to {@link java.lang.Object}.
     *
     * @deprecated Implement your own {@link ColumnVisitor}.
     */
    @Deprecated
    public abstract static class ObjectColumnVisitor implements ColumnVisitor {
        private final PageReader record;
        private final boolean useInstant;

        public ObjectColumnVisitor(final PageReader record, final boolean useInstant) {
            this.record = record;
            this.useInstant = useInstant;
        }

        public ObjectColumnVisitor(PageReader record) {
            this(record, false);
        }

        public abstract void visit(Column column, Object obj);

        @Override
        public void booleanColumn(Column column) {
            if (record.isNull(column)) {
                visit(column, null);
            } else {
                visit(column, record.getBoolean(column));
            }
        }

        @Override
        public void longColumn(Column column) {
            if (record.isNull(column)) {
                visit(column, null);
            } else {
                visit(column, record.getLong(column));
            }
        }

        @Override
        public void doubleColumn(Column column) {
            if (record.isNull(column)) {
                visit(column, null);
            } else {
                visit(column, record.getDouble(column));
            }
        }

        @Override
        public void stringColumn(Column column) {
            if (record.isNull(column)) {
                visit(column, null);
            } else {
                visit(column, record.getString(column));
            }
        }

        @Override
        @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
        public void timestampColumn(Column column) {
            if (record.isNull(column)) {
                visit(column, null);
            } else {
                if (this.useInstant) {
                    visit(column, record.getTimestampInstant(column));
                } else {
                    visit(column, record.getTimestamp(column));
                }
            }
        }

        @Override
        public void jsonColumn(Column column) {
            if (record.isNull(column)) {
                visit(column, null);
            } else {
                visit(column, record.getJson(column));
            }
        }
    }

    public static Object getObject(PageReader record, Column column) {
        GetObjectColumnVisitor visitor = new GetObjectColumnVisitor(record);
        column.visit(visitor);
        return visitor.get();
    }

    private static class GetObjectColumnVisitor extends ObjectColumnVisitor {
        private Object object;

        public GetObjectColumnVisitor(PageReader record) {
            super(record);
        }

        public Object get() {
            return object;
        }

        public void visit(Column column, Object object) {
            this.object = object;
        }
    }
}
