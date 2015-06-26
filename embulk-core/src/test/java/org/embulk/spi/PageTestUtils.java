package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import org.embulk.spi.TestPageBuilderReader.MockPageOutput;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnConfig;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfig;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.type.Type;
import org.embulk.spi.Exec;

public class PageTestUtils
{
    private PageTestUtils()
    {
    }

    public static Schema newSchema()
    {
        return new Schema(ImmutableList.<Column> of());
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
        return new ColumnConfig(name, type, Exec.newConfigSource());
    }

    public static List<Page> buildPage(BufferAllocator bufferAllocator,
            Schema schema, Object... values)
    {
        MockPageOutput output = new MockPageOutput();
        try (PageBuilder builder = new PageBuilder(bufferAllocator, schema,
                output)) {
            int idx = 0;
            while (idx < values.length) {
                for (int column = 0; column < builder.getSchema()
                        .getColumnCount(); ++column) {
                    Object value = values[idx++];
                    if (value == null) {
                        builder.setNull(column);
                    } else if (value instanceof Boolean) {
                        builder.setBoolean(column, (Boolean) value);
                    } else if (value instanceof Double) {
                        builder.setDouble(column, (Double) value);
                    } else if (value instanceof Long) {
                        builder.setLong(column, (Long) value);
                    } else if (value instanceof String) {
                        builder.setString(column, (String) value);
                    } else if (value instanceof Timestamp) {
                        builder.setTimestamp(column, (Timestamp) value);
                    } else {
                        throw new IllegalStateException(
                                "Unsupported type in test utils: "
                                        + value.toString());
                    }
                }
                builder.addRecord();
            }
            builder.finish();
        }
        return output.pages;
    }
}
