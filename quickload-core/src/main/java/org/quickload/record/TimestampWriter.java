package org.quickload.record;

import java.sql.Timestamp;

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
        page.setLong(offset, value.getTime() / 1000);
        page.setInt(offset + 8, value.getNanos());
    }

    @Override
    void callRecordWriter(RecordWriter visitor)
    {
        visitor.writeTimestamp(column, this);
    }
}
