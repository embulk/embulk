package org.embulk.spi.type;

public class LongType
        extends AbstractType
{
    static final LongType LONG = new LongType();

    private LongType()
    {
        super("long", long.class, 8);
    }
}
