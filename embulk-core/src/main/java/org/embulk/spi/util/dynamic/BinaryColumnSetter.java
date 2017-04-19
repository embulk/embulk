package org.embulk.spi.util.dynamic;

import com.google.common.collect.ImmutableSet;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampFormatter;
import org.msgpack.value.Value;

import java.nio.charset.Charset;

public class BinaryColumnSetter
        extends AbstractDynamicColumnSetter
{
    private final TimestampFormatter timestampFormatter;

    public BinaryColumnSetter(PageBuilder pageBuilder, Column column,
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
    public void set(byte[] v)
    {
        pageBuilder.setBinary(column, v);
    }

    @Override
    public void set(boolean v)
    {
        pageBuilder.setBinary(column, Boolean.toString(v).getBytes(Charset.defaultCharset()));
    }

    @Override
    public void set(long v)
    {
        pageBuilder.setBinary(column, Long.toString(v).getBytes(Charset.defaultCharset()));
    }

    @Override
    public void set(double v)
    {
        pageBuilder.setBinary(column, Double.toString(v).getBytes(Charset.defaultCharset()));
    }

    @Override
    public void set(String v)
    {
        pageBuilder.setBinary(column, v.getBytes(Charset.defaultCharset()));
    }

    @Override
    public void set(Timestamp v)
    {
        pageBuilder.setBinary(column, timestampFormatter.format(v).getBytes(Charset.defaultCharset()));
    }

    @Override
    public void set(Value v)
    {
        pageBuilder.setBinary(column, v.toJson().getBytes(Charset.defaultCharset()));
    }
}
