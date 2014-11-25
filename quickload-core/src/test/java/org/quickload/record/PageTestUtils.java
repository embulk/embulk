package org.quickload.record;

import com.google.common.collect.ImmutableList;

public class PageTestUtils
{
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
