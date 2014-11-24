package org.quickload.record;

public class StringReader
        extends TypeReader
{
    public StringReader(PageReader reader, Column column)
    {
        super(reader, column);
    }

    @Override
    void callRecordReader(RecordReader visitor)
    {
        if (reader.isNull(column.getIndex())) {
            visitor.readNull(column);
        } else {
            visitor.readString(column, reader.getString(column.getIndex()));
        }
    }
}
