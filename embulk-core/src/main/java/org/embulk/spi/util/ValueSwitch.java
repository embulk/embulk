package org.embulk.spi.util;

import org.embulk.spi.Column;
import org.embulk.spi.type.ValueConsumer;
import org.embulk.spi.time.Timestamp;

public abstract class ValueSwitch
        implements ValueConsumer
{
    public abstract void whenNull(Column column);

    public abstract void whenElse(Column column, Object value);

    public void whenBoolean(Column column, boolean value)
    {
        whenElse(column, value);
    }

    public void whenLong(Column column, long value)
    {
        whenElse(column, value);
    }

    public void whenDouble(Column column, double value)
    {
        whenElse(column, value);
    }

    public void whenString(Column column, String value)
    {
        whenElse(column, value);
    }

    public void whenTimestamp(Column column, Timestamp value)
    {
        whenElse(column, value);
    }
}
