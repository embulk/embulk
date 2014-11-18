package org.quickload.record;

public class LongReader
        extends TypeReader
{
    public LongReader(PageReader reader, Column column)
    {
        super(reader, column);
    }

    @Override
    void callRecordReader(RecordReader visitor)
    {
        if (reader.isNull(column.getIndex())) {
            visitor.readNull(column);
        } else {
            visitor.readLong(column, reader.getPage().getLong(reader.getOffset(column.getIndex())));
        }
    }
}
