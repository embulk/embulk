package org.embulk.spi.util;

import java.time.Instant;
import org.msgpack.value.Value;

public interface DynamicColumnSetter {
    void setNull();

    void set(boolean value);

    void set(long value);

    void set(double value);

    void set(String value);

    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    void set(org.embulk.spi.time.Timestamp value);

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    default void set(Instant value) {
        this.set(org.embulk.spi.time.Timestamp.ofInstant(value));
    }

    void set(Value value);
}
