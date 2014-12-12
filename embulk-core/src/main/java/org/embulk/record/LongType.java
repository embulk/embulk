package org.embulk.record;

public class LongType
        extends AbstractType
{
    static final LongType LONG = new LongType();

    private LongType()
    {
        super("long", long.class, 8);
    }

    @Override
    public TypeWriter newWriter(PageBuilder builder, Column column)
    {
        return new LongWriter(builder, column);
    }

    @Override
    public TypeReader newReader(PageReader reader, Column column)
    {
        return new LongReader(reader, column);
    }
}
