package org.embulk.spi.util.dynamic;

import java.time.Instant;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.json.JsonBoolean;
import org.embulk.spi.json.JsonDouble;
import org.embulk.spi.json.JsonLong;
import org.embulk.spi.json.JsonString;
import org.embulk.spi.json.JsonValue;
import org.msgpack.value.Value;

public class JsonColumnSetter extends AbstractDynamicColumnSetter {
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1298
    private final org.embulk.spi.time.TimestampFormatter timestampFormatter;

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1298
    public JsonColumnSetter(PageBuilder pageBuilder, Column column,
                            DefaultValueSetter defaultValue,
                            org.embulk.spi.time.TimestampFormatter timestampFormatter) {
        super(pageBuilder, column, defaultValue);
        this.timestampFormatter = timestampFormatter;
    }

    @Override
    public void setNull() {
        pageBuilder.setNull(column);
    }

    @Override
    public void set(boolean v) {
        pageBuilder.setJson(column, JsonBoolean.of(v));
    }

    @Override
    public void set(long v) {
        pageBuilder.setJson(column, JsonLong.of(v));
    }

    @Override
    public void set(double v) {
        pageBuilder.setJson(column, JsonDouble.of(v));
    }

    @Override
    public void set(String v) {
        pageBuilder.setJson(column, JsonString.of(v));
    }

    @Override
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void set(final org.embulk.spi.time.Timestamp v) {
        pageBuilder.setJson(column, JsonString.of(timestampFormatter.format(v)));
    }

    @Override
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void set(Instant v) {
        pageBuilder.setJson(column, JsonString.of(timestampFormatter.format(org.embulk.spi.time.Timestamp.ofInstant(v))));
    }

    @Deprecated
    @Override
    public void set(Value v) {
        pageBuilder.setJson(column, JsonValue.fromMsgpack(v));
    }

    @Override
    public void set(final JsonValue v) {
        pageBuilder.setJson(column, v);
    }
}
