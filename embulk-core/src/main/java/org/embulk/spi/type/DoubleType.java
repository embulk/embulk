package org.embulk.spi.type;

public class DoubleType
        extends AbstractType
{
    static final DoubleType DOUBLE = new DoubleType();

    private DoubleType()
    {
        super("double", double.class, 8);
    }
}
