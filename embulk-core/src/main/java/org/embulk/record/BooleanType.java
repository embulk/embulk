package org.embulk.record;

public class BooleanType
        extends AbstractType
{
    static final BooleanType BOOLEAN = new BooleanType();

    private BooleanType()
    {
        super("boolean", boolean.class, 1);
    }

    @Override
    public TypeWriter newWriter(PageBuilder builder, Column column)
    {
        return new BooleanWriter(builder, column);
    }

    @Override
    public TypeReader newReader(PageReader reader, Column column)
    {
        return new BooleanReader(reader, column);
    }
}
