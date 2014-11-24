package org.quickload.record;

public class DoubleReader
        extends TypeReader
{
    public DoubleReader(PageReader reader, Column column)
    {
        super(reader, column);
    }

    @Override
    void callRecordReader(RecordReader visitor)
    {
        if (reader.isNull(column.getIndex())) {
            visitor.readNull(column);
        } else {
            visitor.readDouble(column, reader.getDouble(column.getIndex()));
        }
    }
}
