package org.embulk.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.embulk.spi.type.Type;

public class SchemaConfig {
    private final List<ColumnConfig> columns;

    public SchemaConfig(List<ColumnConfig> columns) {
        this.columns = columns;
    }

    public List<ColumnConfig> getColumns() {
        return columns;
    }

    public int size() {
        return columns.size();
    }

    public int getColumnCount() {
        return columns.size();
    }

    public ColumnConfig getColumn(int index) {
        return columns.get(index);
    }

    public String getColumnName(int index) {
        return getColumn(index).getName();
    }

    public Type getColumnType(int index) {
        return getColumn(index).getType();
    }

    public boolean isEmpty() {
        return columns.isEmpty();
    }

    public ColumnConfig lookupColumn(String name) {
        for (ColumnConfig c : columns) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        throw new SchemaConfigException(String.format("Column '%s' is not found", name));
    }

    public Schema toSchema() {
        final ArrayList<Column> builder = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            builder.add(columns.get(i).toColumn(i));
        }
        return new Schema(Collections.unmodifiableList(builder));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SchemaConfig)) {
            return false;
        }
        SchemaConfig other = (SchemaConfig) obj;
        return Objects.equals(columns, other.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(columns);
    }
}
