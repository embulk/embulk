package org.embulk.record;

import org.embulk.time.Timestamp;

public interface RecordReader
{
    public void readNull(Column column);

    public void readBoolean(Column column, boolean value);

    public void readLong(Column column, long value);

    public void readDouble(Column column, double value);

    public void readString(Column column, String value);

    public void readTimestamp(Column column, Timestamp value);
}
