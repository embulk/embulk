package org.quickload.record;

public class StringType
        extends AbstractType
{
    static final StringType STRING = new StringType();

    private StringType()
    {
        super("string", String.class, 4);
    }

    @Override
    public TypeWriter newWriter(PageBuilder builder, Column column)
    {
        return new StringWriter(builder, column);
    }

    @Override
    public TypeReader newReader(PageReader reader, Column column)
    {
        return new StringReader(reader, column);
    }
}
