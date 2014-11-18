package org.quickload.record;

public class LongWriter
        extends TypeWriter
{
    private final PageBuilder builder;
    private final Column column;

    public LongWriter(PageBuilder builder, Column column)
    {
        this.builder = builder;
        this.column = column;
    }

    public void writeNull()
    {
        builder.setNull(column.getIndex());
    }

    public void write(long value)
    {
        builder.getPage().setLong(builder.getOffset(column.getIndex()), value);
    }

    @Override
    void callRecordWriter(RecordWriter visitor)
    {
        visitor.writeLong(column, this);
    }
}
