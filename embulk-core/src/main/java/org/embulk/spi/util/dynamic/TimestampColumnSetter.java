package org.embulk.spi.util.dynamic;

import org.embulk.spi.PageBuilder;
import org.embulk.spi.Column;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.time.TimestampParseException;
import org.msgpack.value.Value;

public class TimestampColumnSetter
        extends AbstractDynamicColumnSetter
{
    private final TimestampParser timestampParser;

    public TimestampColumnSetter(PageBuilder pageBuilder, Column column,
            DefaultValueSetter defaultValue,
            TimestampParser timestampParser)
    {
        super(pageBuilder, column, defaultValue);
        this.timestampParser = timestampParser;
    }

    @Override
    public void setNull()
    {
        pageBuilder.setNull(column);
    }

    @Override
    public void set(boolean v)
    {
        defaultValue.setTimestamp(pageBuilder, column);
    }

    @Override
    public void set(long v)
    {
        pageBuilder.setTimestamp(column, Timestamp.ofEpochSecond(v));
    }

    @Override
    public void set(double v)
    {
        long sec = (long) v;
        int nsec = (int) ((v - (double) sec) * 1000000000);
        pageBuilder.setTimestamp(column, Timestamp.ofEpochSecond(sec, nsec));
        defaultValue.setTimestamp(pageBuilder, column);
    }

    @Override
    public void set(String v)
    {
        try {
            pageBuilder.setTimestamp(column, timestampParser.parse(v));
        }
        catch (TimestampParseException e) {
            defaultValue.setTimestamp(pageBuilder, column);
        }
    }

    @Override
    public void set(Timestamp v)
    {
        pageBuilder.setTimestamp(column, v);
    }

    @Override
    public void set(Value v)
    {
        defaultValue.setTimestamp(pageBuilder, column);
    }
}
