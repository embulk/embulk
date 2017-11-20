package org.embulk.spi.util.dynamic;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.time.Timestamp;
import org.msgpack.value.Value;

public class DoubleColumnSetter
        extends AbstractDynamicColumnSetter
{
    public DoubleColumnSetter(PageBuilder pageBuilder, Column column,
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
        pageBuilder.setDouble(column, v ? 1.0 : 0.0);
    }

    @Override
    public void set(long v)
    {
        pageBuilder.setDouble(column, (double) v);
    }

    @Override
    public void set(double v)
    {
        pageBuilder.setDouble(column, v);
    }

    @Override
    public void set(String v)
    {
        double dv;
        try {
            dv = Double.parseDouble(v);
        } catch (NumberFormatException e) {
            defaultValue.setDouble(pageBuilder, column);
            return;
        }
        pageBuilder.setDouble(column, dv);
    }

    @Override
    public void set(ByteBuffer v)
    {
        // Charset.defaultCharset should not be used. It depends on the runtime environment.
        this.set(StandardCharsets.UTF_8.decode(v).toString());
    }

    @Override
    public void set(Timestamp v)
    {
        double sec = (double) v.getEpochSecond();
        double frac = v.getNano() / 1000000000.0;
        pageBuilder.setDouble(column, sec + frac);
    }

    @Override
    public void set(Value v)
    {
        defaultValue.setDouble(pageBuilder, column);
    }
}
