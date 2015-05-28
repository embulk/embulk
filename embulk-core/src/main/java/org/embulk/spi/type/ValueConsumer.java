package org.embulk.spi.type;

import org.embulk.spi.Column;
import org.embulk.spi.type.LongType;
import org.embulk.spi.type.StringType;
import org.embulk.spi.time.Timestamp;

public interface ValueConsumer
{
    public void whenNull(Column column);

    public void whenBoolean(Column column, boolean value);

    public void whenLong(Column column, long value);

    public void whenDouble(Column column, double value);

    public void whenString(Column column, String value);

    public void whenTimestamp(Column column, Timestamp value);
}
