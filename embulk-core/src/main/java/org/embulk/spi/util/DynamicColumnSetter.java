package org.embulk.spi.util;

import org.embulk.spi.time.Timestamp;

public interface DynamicColumnSetter
{
    public void setNull();

    public void set(boolean value);

    public void set(long value);

    public void set(double value);

    public void set(String value);

    public void set(Timestamp value);
}
