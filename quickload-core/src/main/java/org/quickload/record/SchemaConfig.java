package org.quickload.record;

import java.util.List;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class SchemaConfig
{
    private final List<ColumnConfig> columns;

    @JsonCreator
    public SchemaConfig(List<ColumnConfig> columns)
    {
        this.columns = columns;
    }

    @JsonValue
    public List<ColumnConfig> getColumns()
    {
        return columns;
    }

    public Schema toSchema()
    {
        ImmutableList.Builder<Column> builder = ImmutableList.builder();
        for (int i=0; i < columns.size(); i++) {
            builder.add(columns.get(i).toColumn(i));
        }
        return new Schema(builder.build());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SchemaConfig)) {
            return false;
        }
        SchemaConfig other = (SchemaConfig) obj;
        return Objects.equal(columns, other.columns);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(columns);
    }
}
