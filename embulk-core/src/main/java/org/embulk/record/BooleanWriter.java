package org.embulk.record;

public class BooleanWriter
        extends TypeWriter
{
    public BooleanWriter(PageBuilder builder, Column column)
    {
        super(builder, column);
    }

    public void writeNull()
    {
        builder.setNull(column.getIndex());
    }

    public void write(boolean value)
    {
        builder.setBoolean(column.getIndex(), value);
    }

    @Override
    void callRecordWriter(RecordWriter visitor)
    {
        visitor.writeBoolean(column, this);
    }
}
