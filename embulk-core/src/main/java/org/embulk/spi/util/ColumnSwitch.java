package org.embulk.spi.util;

import org.embulk.spi.Column;
import org.embulk.spi.type.ValueProducer;
import org.embulk.spi.type.LongType;
import org.embulk.spi.type.BooleanType;
import org.embulk.spi.type.DoubleType;
import org.embulk.spi.type.StringType;
import org.embulk.spi.type.TimestampType;

public abstract class ColumnSwitch <C>
        implements ValueProducer<C>
{
    public abstract void whenBoolean(C context, Column column, BooleanType.Sink sink);

    public abstract void whenLong(C context, Column column, LongType.Sink sink);

    public abstract void whenDouble(C context, Column column, DoubleType.Sink sink);

    public abstract void whenString(C context, Column column, StringType.Sink sink);

    public abstract void whenTimestamp(C context, Column column, TimestampType.Sink sink);
}
