package org.quickload.record;

public class DoubleWriter
        extends TypeWriter
{
    private final PageBuilder builder;
    private final Column column;

    public DoubleWriter(PageBuilder builder, Column column)
    {
        this.builder = builder;
        this.column = column;
    }

    public void writeNull()
    {
        builder.setNull(column.getIndex());
    }

    public void write(double value)
    {
        builder.getPage().setDouble(builder.getOffset(column.getIndex()), value);
    }

    @Override
    void callRecordWriter(RecordWriter visitor)
    {
        visitor.writeDouble(column, this);
    }
}
