package org.embulk.spi.util.dynamic;

import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.time.Timestamp;
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

    public abstract void set(Timestamp value);

    public abstract void set(Value value);
}
