package org.embulk.record;

public class DoubleWriter
        extends TypeWriter
{
    public DoubleWriter(PageBuilder builder, Column column)
    {
        super(builder, column);
    }

    public void writeNull()
    {
        builder.setNull(column.getIndex());
    }

    public void write(double value)
    {
        builder.setDouble(column.getIndex(), value);
    }

    @Override
    void callRecordWriter(RecordWriter visitor)
    {
        visitor.writeDouble(column, this);
    }
}
