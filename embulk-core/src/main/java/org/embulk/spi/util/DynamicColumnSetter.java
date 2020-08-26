package org.embulk.spi.util;

import java.time.Instant;
import org.embulk.spi.time.Timestamp;
import org.msgpack.value.Value;

public interface DynamicColumnSetter {
    void setNull();

    void set(boolean value);

    void set(long value);

    void set(double value);

    void set(String value);

    @Deprecated
    @SuppressWarnings("deprecation")
    void set(Timestamp value);

    default void set(Instant value) {
        this.set(Timestamp.ofInstant(value));
    }

    void set(Value value);
}
