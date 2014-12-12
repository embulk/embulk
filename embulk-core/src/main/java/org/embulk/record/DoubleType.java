package org.embulk.record;

public class DoubleType
        extends AbstractType
{
    static final DoubleType DOUBLE = new DoubleType();

    private DoubleType()
    {
        super("double", double.class, 8);
    }

    @Override
    public TypeWriter newWriter(PageBuilder builder, Column column)
    {
        return new DoubleWriter(builder, column);
    }

    @Override
    public TypeReader newReader(PageReader reader, Column column)
    {
        return new DoubleReader(reader, column);
    }
}
