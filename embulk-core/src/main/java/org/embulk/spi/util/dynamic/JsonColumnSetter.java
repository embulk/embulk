package org.embulk.spi.util.dynamic;

import java.time.Instant;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

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
        pageBuilder.setJson(column, ValueFactory.newBoolean(v));
    }

    @Override
    public void set(long v) {
        pageBuilder.setJson(column, ValueFactory.newInteger(v));
    }

    @Override
    public void set(double v) {
        pageBuilder.setJson(column, ValueFactory.newFloat(v));
    }

    @Override
    public void set(String v) {
        pageBuilder.setJson(column, ValueFactory.newString(v));
    }

    @Override
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void set(final org.embulk.spi.time.Timestamp v) {
        pageBuilder.setJson(column, ValueFactory.newString(timestampFormatter.format(v)));
    }

    @Override
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void set(Instant v) {
        pageBuilder.setJson(column, ValueFactory.newString(timestampFormatter.format(org.embulk.spi.time.Timestamp.ofInstant(v))));
    }

    @Override
    public void set(Value v) {
        pageBuilder.setJson(column, v);
    }
}
