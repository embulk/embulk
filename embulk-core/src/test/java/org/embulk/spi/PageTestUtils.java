package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;

import org.embulk.spi.TestPageBuilderReader.MockPageOutput;
import org.embulk.time.Timestamp;
import org.embulk.type.Column;
import org.embulk.type.ColumnConfig;
import org.embulk.type.Schema;
import org.embulk.type.SchemaConfig;
import org.embulk.type.SchemaVisitor;
import org.embulk.type.Type;

import com.google.common.collect.ImmutableList;

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
        return new ColumnConfig(name, type, null);
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

    public static List<List<Object>> parsePage(Schema schema, Page page)
    {
        List<List<Object>> records = new ArrayList<>();
        try (final PageReader reader = new PageReader(schema)) {
            reader.setPage(page);
            while (reader.nextRecord()) {
                final List<Object> record = new ArrayList<>();
                reader.getSchema().visitColumns(new SchemaVisitor()
                {
                    @Override
                    public void booleanColumn(Column column)
                    {
                        if (reader.isNull(column)) {
                            record.add(null);
                        } else {
                            record.add(reader.getBoolean(column));
                        }
                    }

                    @Override
                    public void longColumn(Column column)
                    {
                        if (reader.isNull(column)) {
                            record.add(null);
                        } else {
                            record.add(reader.getLong(column));
                        }
                    }

                    @Override
                    public void doubleColumn(Column column)
                    {
                        if (reader.isNull(column)) {
                            record.add(null);
                        } else {
                            record.add(reader.getDouble(column));
                        }
                    }

                    @Override
                    public void stringColumn(Column column)
                    {
                        if (reader.isNull(column)) {
                            record.add(null);
                        } else {
                            record.add(reader.getString(column));
                        }
                    }

                    @Override
                    public void timestampColumn(Column column)
                    {
                        if (reader.isNull(column)) {
                            record.add(null);
                        } else {
                            record.add(reader.getTimestamp(column));
                        }
                    }
                });
                records.add(record);
            }
        }
        return records;
    }
}
