package org.quickload.record;

import org.quickload.time.Timestamp;

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
            Page page = reader.getPage();
            int offset = reader.getOffset(reader.getOffset(column.getIndex()));
            long msec = page.getLong(offset);
            int nsec = page.getInt(offset + 8);
            Timestamp value = Timestamp.ofEpochSecond(msec, nsec);
            visitor.readTimestamp(column, value);
        }
    }
}
