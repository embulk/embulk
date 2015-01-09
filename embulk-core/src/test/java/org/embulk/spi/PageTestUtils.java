package org.embulk.spi;

import com.google.common.collect.ImmutableList;
import org.embulk.type.Type;
import org.embulk.type.Schema;
import org.embulk.type.SchemaConfig;
import org.embulk.type.Column;
import org.embulk.type.ColumnConfig;

public class PageTestUtils
{
    private PageTestUtils() { }

    public static Schema newSchema()
    {
        return new Schema(ImmutableList.<Column>of());
    }

    public static Schema newSchema(Column... columns)
    {
        return new Schema(ImmutableList.copyOf(columns));
    }

    public static Schema newSchema(ColumnConfig... columns)
    {
        return new SchemaConfig(ImmutableList.copyOf(columns)).toSchema();
    }

    public static ColumnConfig newColumn(String name, Type type)
    {
        return new ColumnConfig(name, type, null);
    }
}
