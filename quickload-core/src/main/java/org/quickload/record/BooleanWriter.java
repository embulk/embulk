package org.quickload.record;

public class BooleanWriter
        extends TypeWriter
{
    private final PageBuilder builder;
    private final Column column;

    public BooleanWriter(PageBuilder builder, Column column)
    {
        this.builder = builder;
        this.column = column;
    }

    public void writeNull()
    {
        builder.setNull(column.getIndex());
    }

    public void write(boolean value)
    {
        builder.getPage().setByte(builder.getOffset(column.getIndex()), value ? (byte) 1 : (byte) 0);
    }

    @Override
    void callRecordWriter(RecordWriter visitor)
    {
        visitor.writeBoolean(column, this);
    }
}
