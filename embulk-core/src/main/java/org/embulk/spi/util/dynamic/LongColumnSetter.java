package org.embulk.spi.util.dynamic;

import java.math.RoundingMode;
import com.google.common.math.DoubleMath;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.time.Timestamp;
import org.msgpack.value.Value;

public class LongColumnSetter
        extends AbstractDynamicColumnSetter
{
    public LongColumnSetter(PageBuilder pageBuilder, Column column,
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
        pageBuilder.setLong(column, v ? 1L : 0L);
    }

    @Override
    public void set(long v)
    {
        pageBuilder.setLong(column, v);
    }

    @Override
    public void set(double v)
    {
        long lv;
        try {
            // TODO configurable rounding mode
            lv = DoubleMath.roundToLong(v, RoundingMode.HALF_UP);
        }
        catch (ArithmeticException ex) {
            // NaN / Infinite / -Infinite
            defaultValue.setLong(pageBuilder, column);
            return;
        }
        pageBuilder.setLong(column, lv);
    }

    @Override
    public void set(String v)
    {
        long lv;
        try {
            lv = Long.parseLong(v);
        }
        catch (NumberFormatException e) {
            defaultValue.setLong(pageBuilder, column);
            return;
        }
        pageBuilder.setLong(column, lv);
    }

    @Override
    public void set(Timestamp v)
    {
        pageBuilder.setLong(column, v.getEpochSecond());
    }

    @Override
    public void set(Value v)
    {
        defaultValue.setLong(pageBuilder, column);
    }
}
