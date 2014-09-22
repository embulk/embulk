package org.quickload.record;

public abstract class AbstractType
        implements Type
{
    public boolean isNull(RecordCursor cursor, int columnIndex)
    {
        return cursor.isNull(columnIndex);
    }

    public void setNull(RecordBuilder builder, int columnIndex)
    {
        builder.setNull(columnIndex);
    }

    public long getLong(RecordCursor cursor, int columnIndex)
    {
        // TODO exception class
        throw new RuntimeException("type mismatch");
    }

    public double getDouble(RecordCursor cursor, int columnIndex)
    {
        // TODO exception class
        throw new RuntimeException("type mismatch");
    }

    public String getString(RecordCursor cursor, int columnIndex)
    {
        // TODO exception class
        throw new RuntimeException("type mismatch");
    }

    public void setLong(RecordBuilder builder, int columnIndex, long value)
    {
        // TODO exception class
        throw new RuntimeException("type mismatch");
    }

    public void setDouble(RecordBuilder builder, int columnIndex, double value)
    {
        // TODO exception class
        throw new RuntimeException("type mismatch");
    }

    public void setString(RecordBuilder builder, int columnIndex, String value)
    {
        // TODO exception class
        throw new RuntimeException("type mismatch");
    }
}
