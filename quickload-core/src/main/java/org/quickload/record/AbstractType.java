package org.quickload.record;

public abstract class AbstractType
        implements Type
{
    public boolean isNull(RecordCursor cursor, int columnIndex)
    {
        return cursor.isNull(columnIndex);
    }

    public void setNull(PageBuilder builder, int columnIndex)
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

    public void setLong(PageBuilder builder, int columnIndex, long value)
    {
        // TODO exception class
        throw new RuntimeException("type mismatch");
    }

    public void setDouble(PageBuilder builder, int columnIndex, double value)
    {
        // TODO exception class
        throw new RuntimeException("type mismatch");
    }

    public void setString(PageBuilder builder, int columnIndex, String value)
    {
        // TODO exception class
        throw new RuntimeException("type mismatch");
    }
}
