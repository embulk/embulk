package org.embulk.spi.util.dynamic;

import java.time.Instant;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.util.DynamicColumnSetter;
import org.msgpack.value.Value;

public abstract class AbstractDynamicColumnSetter implements DynamicColumnSetter {
    protected final PageBuilder pageBuilder;
    protected final Column column;
    protected final DefaultValueSetter defaultValue;

    protected AbstractDynamicColumnSetter(PageBuilder pageBuilder, Column column,
            DefaultValueSetter defaultValue) {
        this.pageBuilder = pageBuilder;
        this.column = column;
        this.defaultValue = defaultValue;
    }

    public abstract void setNull();

    public abstract void set(boolean value);

    public abstract void set(long value);

    public abstract void set(double value);

    public abstract void set(String value);

    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public abstract void set(org.embulk.spi.time.Timestamp value);

    @Override
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void set(Instant value) {
        this.set(org.embulk.spi.time.Timestamp.ofInstant(value));
    }

    public abstract void set(Value value);
}
