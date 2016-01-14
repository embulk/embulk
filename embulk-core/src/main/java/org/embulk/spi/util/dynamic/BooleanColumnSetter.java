package org.embulk.spi.util.dynamic;

import com.google.common.collect.ImmutableSet;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.time.Timestamp;
import org.msgpack.value.Value;

public class BooleanColumnSetter
        extends AbstractDynamicColumnSetter
{
    private static final ImmutableSet<String> TRUE_STRINGS =
        ImmutableSet.of(
                "true", "True", "TRUE",
                "yes", "Yes", "YES",
                "t", "T", "y", "Y",
                "on", "On", "ON",
                "1");

    public BooleanColumnSetter(PageBuilder pageBuilder, Column column,
            DefaultValueSetter defaultValue)
    {
        super(pageBuilder, column, defaultValue);
    }

    @Override
    public void setNull()
    {
        pageBuilder.setNull(column);
    }

    @Override
    public void set(boolean v)
    {
        pageBuilder.setBoolean(column, v);
    }

    @Override
    public void set(long v)
    {
        pageBuilder.setBoolean(column, v > 0);
    }

    @Override
    public void set(double v)
    {
        pageBuilder.setBoolean(column, v > 0.0);
    }

    @Override
    public void set(String v)
    {
        if (TRUE_STRINGS.contains(v)) {
            pageBuilder.setBoolean(column, true);
        } else {
            defaultValue.setBoolean(pageBuilder, column);
        }
    }

    @Override
    public void set(Timestamp v)
    {
        defaultValue.setBoolean(pageBuilder, column);
    }

    @Override
    public void set(Value v)
    {
        defaultValue.setBoolean(pageBuilder, column);
    }
}
