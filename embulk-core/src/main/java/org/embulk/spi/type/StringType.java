package org.embulk.spi.type;

public class StringType
        extends AbstractType
{
    static final StringType STRING = new StringType();

    private StringType()
    {
        super("string", String.class, 4);
    }
}
