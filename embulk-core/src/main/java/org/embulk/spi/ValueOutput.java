package org.embulk.spi;

import org.msgpack.value.Value;

public interface ValueOutput
        extends AutoCloseable
{
    void add(Value value);

    void close();
}

