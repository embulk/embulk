package org.quickload.record;

import com.fasterxml.jackson.annotation.JsonValue;

public interface Type
{
    public static final byte VARIABLE_LENGTH_INDEX_SIZE = (byte) 4;

    @JsonValue
    public String getName();

    public Class<?> getJavaType();

    public byte getFixedStorageSize();

    public void consume(RecordCursor cursor, RecordConsumer consumer, Column column);

    public boolean isNull(RecordCursor cursor, int columnIndex);

    public long getLong(RecordCursor cursor, int columnIndex);

    public double getDouble(RecordCursor cursor, int columnIndex);

    public String getString(RecordCursor cursor, int columnIndex);

    public void produce(RecordBuilder builder, RecordProducer producer, Column column);

    public void setNull(RecordBuilder builder, int columnIndex);

    public void setLong(RecordBuilder builder, int columnIndex, long value);

    public void setDouble(RecordBuilder builder, int columnIndex, double value);

    public void setString(RecordBuilder builder, int columnIndex, String value);

    // TODO binary
    // TODO timestamp
    // TODO array?
    // TODO map?

    // TODO
    //public void copyTo(RecordCursor cursor, int srcColumnIndex, RecordBuilder dst, int dstColumnIndex);
}
