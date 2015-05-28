package org.embulk.spi.type;

import org.embulk.spi.Column;
import org.embulk.spi.type.LongType;
import org.embulk.spi.type.BooleanType;
import org.embulk.spi.type.DoubleType;
import org.embulk.spi.type.StringType;
import org.embulk.spi.type.TimestampType;

public interface ValueProducer <C>
{
    public void whenBoolean(C context, Column column, BooleanType.Sink sink);

    public void whenLong(C context, Column column, LongType.Sink sink);

    public void whenDouble(C context, Column column, DoubleType.Sink sink);

    public void whenString(C context, Column column, StringType.Sink sink);

    public void whenTimestamp(C context, Column column, TimestampType.Sink sink);
}
