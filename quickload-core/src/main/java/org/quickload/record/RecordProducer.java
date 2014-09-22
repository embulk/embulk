package org.quickload.record;

public interface RecordProducer
{
    public void setLong(Column column, LongType.Setter setter);

    public void setDouble(Column column, DoubleType.Setter setter);

    public void setString(Column column, StringType.Setter setter);
}
