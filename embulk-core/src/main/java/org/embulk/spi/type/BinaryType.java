package org.embulk.spi.type;

public class BinaryType
        extends AbstractType
{
    static final BinaryType BINARY = new BinaryType();

    private BinaryType()
    {
        super("binary", byte[].class, 4);
    }
}
