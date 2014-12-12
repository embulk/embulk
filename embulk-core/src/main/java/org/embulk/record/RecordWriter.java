package org.embulk.record;

public interface RecordWriter
{
    public void writeBoolean(Column column, BooleanWriter writer);

    public void writeLong(Column column, LongWriter writer);

    public void writeDouble(Column column, DoubleWriter writer);

    public void writeString(Column column, StringWriter writer);

    public void writeTimestamp(Column column, TimestampWriter writer);
}
