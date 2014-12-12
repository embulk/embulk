package org.embulk.record;

public class TimestampReader
        extends TypeReader
{
    public TimestampReader(PageReader reader, Column column)
    {
        super(reader, column);
    }

    @Override
    void callRecordReader(RecordReader visitor)
    {
        if (reader.isNull(column.getIndex())) {
            visitor.readNull(column);
        } else {
            visitor.readTimestamp(column, reader.getTimestamp(column.getIndex()));
        }
    }
}
