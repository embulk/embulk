/*
 * Copyright 2014 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.parser.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.Exec;
import org.embulk.spi.Page;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;

/**
 * A utility class to manupulate {@link org.embulk.spi.Page}s.
 *
 * <p>It is based on a copy from {@code embulk-core}'s {@code org.embulk.spi.util.Pages}.
 */
class Pages {
    static List<Object[]> toObjects(final Schema schema, final Iterable<Page> pages) {
        final ArrayList<Object[]> builder = new ArrayList<>();
        Iterator<Page> ite = pages.iterator();
        try (PageReader reader = Exec.getPageReader(schema)) {
            while (ite.hasNext()) {
                reader.setPage(ite.next());
                while (reader.nextRecord()) {
                    builder.add(toObjects(reader));
                }
            }
        }
        return Collections.unmodifiableList(builder);
    }

    private static Object[] toObjects(final PageReader record) {
        final Object[] values = new Object[record.getSchema().getColumns().size()];
        record.getSchema().visitColumns(new ObjectColumnVisitor(record) {
                @Override
                public void visit(Column column, Object object) {
                    values[column.getIndex()] = object;
                }
            });
        return values;
    }

    private abstract static class ObjectColumnVisitor implements ColumnVisitor {
        private final PageReader record;

        ObjectColumnVisitor(final PageReader record) {
            this.record = record;
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
        public void timestampColumn(Column column) {
            if (record.isNull(column)) {
                visit(column, null);
            } else {
                visit(column, record.getTimestampInstant(column));
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
}
