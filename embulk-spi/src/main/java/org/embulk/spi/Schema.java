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

package org.embulk.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.embulk.spi.type.Type;

/**
 * Represents a schema of Embulk's data record.
 *
 * @since 0.4.0
 */
public class Schema {
    /**
     * @since 0.6.14
     */
    public static class Builder {
        public Builder() {
            this.index = 0;
        }

        /**
         * @since 0.6.14
         */
        public synchronized Builder add(final String name, final Type type) {
            this.columns.add(new Column(this.index++, name, type));
            return this;
        }

        /**
         * @since 0.6.14
         */
        public Schema build() {
            return new Schema(Collections.unmodifiableList(this.columns));
        }

        private final ArrayList<Column> columns = new ArrayList<>();

        private int index;
    }

    /**
     * @since 0.6.14
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @since 0.4.0
     */
    public Schema(final List<Column> columns) {
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
    }

    /**
     * Returns the list of Column objects.
     *
     * It always returns an immutable list.
     *
     * @since 0.4.0
     */
    public List<Column> getColumns() {
        return this.columns;
    }

    /**
     * @since 0.4.0
     */
    public int size() {
        return this.columns.size();
    }

    /**
     * @since 0.4.0
     */
    public int getColumnCount() {
        return this.columns.size();
    }

    /**
     * @since 0.4.0
     */
    public Column getColumn(final int index) {
        return this.columns.get(index);
    }

    /**
     * @since 0.4.0
     */
    public String getColumnName(final int index) {
        return this.getColumn(index).getName();
    }

    /**
     * @since 0.4.0
     */
    public Type getColumnType(final int index) {
        return this.getColumn(index).getType();
    }

    /**
     * @since 0.4.0
     */
    public void visitColumns(final ColumnVisitor visitor) {
        for (final Column column : this.columns) {
            column.visit(visitor);
        }
    }

    /**
     * @since 0.4.0
     */
    public boolean isEmpty() {
        return this.columns.isEmpty();
    }

    /**
     * @since 0.6.14
     */
    public Column lookupColumn(final String name) {
        for (final Column c : this.columns) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        throw new SchemaConfigException(String.format("Column '%s' is not found", name));
    }

    /**
     * @since 0.4.0
     */
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1324
    public int getFixedStorageSize() {
        int total = 0;
        for (final Column column : this.columns) {
            total += column.getType().getFixedStorageSize();
        }
        return total;
    }

    /**
     * @since 0.4.0
     */
    @Override
    public boolean equals(final Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof Schema)) {
            return false;
        }
        final Schema other = (Schema) otherObject;
        return Objects.equals(this.columns, other.columns);
    }

    /**
     * @since 0.4.0
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.columns);
    }

    /**
     * @since 0.4.0
     */
    @Override
    public String toString() {
        final StringBuilder sbuf = new StringBuilder();
        sbuf.append("Schema{\n");
        for (final Column c : this.columns) {
            sbuf.append(String.format(" %4d: %s %s%n", c.getIndex(), c.getName(), c.getType()));
        }
        sbuf.append("}");
        return sbuf.toString();
    }

    private final List<Column> columns;
}
