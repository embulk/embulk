package org.quickload.record;

import java.sql.Timestamp;

public class TimestampReader
        extends TypeReader
{
    private final PageReader reader;
    private final Column column;

    public TimestampReader(PageReader reader, Column column)
    {
        this.reader = reader;
        this.column = column;
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
            Timestamp value = new Timestamp(msec);
            value.setNanos(nsec);
            visitor.readTimestamp(column, value);
        }
    }
}
