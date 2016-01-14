package org.embulk.spi.util;

import org.embulk.spi.time.Timestamp;
import org.msgpack.value.Value;

public interface DynamicColumnSetter
{
    void setNull();

    void set(boolean value);

    void set(long value);

    void set(double value);

    void set(String value);

    void set(Timestamp value);

    void set(Value value);
}
