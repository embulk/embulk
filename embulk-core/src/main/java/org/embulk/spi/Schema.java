package org.embulk.spi;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.embulk.spi.type.Type;

public class Schema
{
    public static class Builder
    {
        private final ImmutableList.Builder<Column> columns = ImmutableList.builder();
        private int index = 0;  // next version of Guava will have ImmutableList.Builder.size()

        public synchronized Builder add(String name, Type type)
        {
            columns.add(new Column(index++, name, type));
            return this;
        }

        public Schema build()
        {
            return new Schema(columns.build());
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    private final ImmutableList<Column> columns;

    @JsonCreator
    public Schema(List<Column> columns)
    {
        this.columns = ImmutableList.copyOf(columns);
    }

    /**
     * Returns the list of Column objects.
     *
     * It always returns an immutable list.
     */
    @JsonValue
    public List<Column> getColumns()
    {
        return columns;
    }

    public int size()
    {
        return columns.size();
    }

    public int getColumnCount()
    {
        return columns.size();
    }

    public Column getColumn(int index)
    {
        return columns.get(index);
    }

    public String getColumnName(int index)
    {
        return getColumn(index).getName();
    }

    public Type getColumnType(int index)
    {
        return getColumn(index).getType();
    }

    public void visitColumns(ColumnVisitor visitor)
    {
        for (Column column : columns) {
            column.visit(visitor);
        }
    }

    public boolean isEmpty()
    {
        return columns.isEmpty();
    }

    public Column lookupColumn(String name)
    {
        for (Column c : columns) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        throw new SchemaConfigException(String.format("Column '%s' is not found", name));
    }

    public int getFixedStorageSize()
    {
        int total = 0;
        for (Column column : columns) {
            total += column.getType().getFixedStorageSize();
        }
        return total;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Schema)) {
            return false;
        }
        Schema other = (Schema) obj;
        return Objects.equals(columns, other.columns);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(columns);
    }

    @Override
    public String toString()
    {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("Schema{\n");
        for (Column c : columns) {
            sbuf.append(String.format(" %4d: %s %s%n", c.getIndex(), c.getName(), c.getType()));
        }
        sbuf.append("}");
        return sbuf.toString();
    }
}
