package org.embulk.record;

import org.embulk.time.Timestamp;

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
        if (value == null) {
            // this is unnecessary check but exists for buggy plugins
            writeNull();
        } else {
            builder.setTimestamp(column.getIndex(), value);
        }
    }

    @Override
    void callRecordWriter(RecordWriter visitor)
    {
        visitor.writeTimestamp(column, this);
    }
}
