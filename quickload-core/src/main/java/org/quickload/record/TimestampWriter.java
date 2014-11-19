package org.quickload.record;

import org.quickload.time.Timestamp;

public class TimestampWriter
        extends TypeWriter
{
    public TimestampWriter(PageBuilder builder, Column column)
    {
        super(builder, column);
    }

    public void writeNull()
    {
        builder.setNull(column.getIndex());
    }

    public void write(Timestamp value)
    {
        Page page = builder.getPage();
        int offset = builder.getOffset(column.getIndex());
        page.setLong(offset, value.getEpochSecond() / 1000);
        page.setInt(offset + 8, value.getNano());
    }

    @Override
    void callRecordWriter(RecordWriter visitor)
    {
        visitor.writeTimestamp(column, this);
    }
}
