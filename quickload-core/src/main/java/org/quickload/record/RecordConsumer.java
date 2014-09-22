package org.quickload.record;

public interface RecordConsumer
{
    public void setNull(Column column);

    public void setLong(Column column, long value);

    public void setDouble(Column column, double value);

    public void setString(Column column, String value);
}
