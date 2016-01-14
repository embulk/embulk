package org.embulk.spi.util.dynamic;

import org.embulk.spi.PageBuilder;
import org.embulk.spi.Column;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampFormatter;
import org.msgpack.value.Value;

public class StringColumnSetter
        extends AbstractDynamicColumnSetter
{
    private final TimestampFormatter timestampFormatter;

    public StringColumnSetter(PageBuilder pageBuilder, Column column,
            DefaultValueSetter defaultValue,
            TimestampFormatter timestampFormatter)
    {
        super(pageBuilder, column, defaultValue);
        this.timestampFormatter = timestampFormatter;
    }

    @Override
    public void setNull()
    {
        pageBuilder.setNull(column);
    }

    @Override
    public void set(boolean v)
    {
        pageBuilder.setString(column, Boolean.toString(v));
    }

    @Override
    public void set(long v)
    {
        pageBuilder.setString(column, Long.toString(v));
    }

    @Override
    public void set(double v)
    {
        pageBuilder.setString(column, Double.toString(v));
    }

    @Override
    public void set(String v)
    {
        pageBuilder.setString(column, v);
    }

    @Override
    public void set(Timestamp v)
    {
        pageBuilder.setString(column, timestampFormatter.format(v));
    }

    @Override
    public void set(Value v)
    {
        pageBuilder.setString(column, v.toJson());
    }
}
