package org.embulk.spi.util.dynamic;

import java.time.Instant;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.msgpack.value.Value;

public class StringColumnSetter extends AbstractDynamicColumnSetter {
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1298
    private final org.embulk.spi.time.TimestampFormatter timestampFormatter;

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1298
    public StringColumnSetter(PageBuilder pageBuilder, Column column,
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
        pageBuilder.setString(column, Boolean.toString(v));
    }

    @Override
    public void set(long v) {
        pageBuilder.setString(column, Long.toString(v));
    }

    @Override
    public void set(double v) {
        pageBuilder.setString(column, Double.toString(v));
    }

    @Override
    public void set(String v) {
        pageBuilder.setString(column, v);
    }

    @Override
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void set(final org.embulk.spi.time.Timestamp v) {
        pageBuilder.setString(column, timestampFormatter.format(v));
    }

    @Override
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void set(Instant v) {
        pageBuilder.setString(column, timestampFormatter.format(org.embulk.spi.time.Timestamp.ofInstant(v)));
    }

    @Override
    public void set(Value v) {
        pageBuilder.setString(column, v.toJson());
    }
}
