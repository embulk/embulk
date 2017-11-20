package org.embulk.spi.type;

import java.nio.ByteBuffer;

public class BinaryType
        extends AbstractType
{
    static final BinaryType BINARY = new BinaryType();

    private BinaryType()
    {
        super("binary", ByteBuffer.class, 4);
    }
}
