package org.embulk.spi;

import java.util.List;
import org.embulk.spi.TestPageBuilderReader.MockPageOutput;
import org.embulk.spi.time.Timestamp;
import org.msgpack.value.Value;

public class PageTestUtils {
    private PageTestUtils() {}

    public static List<Page> buildPage(BufferAllocator bufferAllocator,
            Schema schema, Object... values) {
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
                    } else if (value instanceof Value) {
                        builder.setJson(column, (Value) value);
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
