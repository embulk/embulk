package org.quickload.spi;

import org.quickload.buffer.Buffer;

public interface BufferOperator
        extends Operator
{
    public void addBuffer(Buffer buffer);
}
