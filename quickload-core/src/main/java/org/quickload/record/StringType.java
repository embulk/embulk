package org.quickload.record;

public class StringType
        extends AbstractType
{
    public static final StringType STRING = new StringType();

    private StringType()
    {
    }

    @Override
    public Class<?> getJavaType()
    {
        return String.class;
    }

    @Override
    public byte getFixedStorageSize()
    {
        return (byte) 4;
    }

    @Override
    public String getString(RecordCursor cursor, int columnIndex)
    {
        return cursor.getString(columnIndex);
    }

    @Override
    public void setString(RecordBuilder builder, int columnIndex, String value)
    {
        builder.setString(columnIndex, value);
    }

    @Override
    public void consume(RecordCursor cursor, RecordConsumer consumer, Column column)
    {
        if (cursor.isNull(column.getIndex())) {
            consumer.setNull(column);
        } else {
            consumer.setString(column, cursor.getString(column.getIndex()));
        }
    }

    @Override
    public void produce(RecordBuilder builder, RecordProducer producer, Column column)
    {
        producer.setString(column, new Setter(builder, column.getIndex()));
    }

    public class Setter
    {
        private final RecordBuilder builder;
        private final int columnIndex;

        Setter(RecordBuilder builder, int columnIndex)
        {
            this.builder = builder;
            this.columnIndex = columnIndex;
        }

        public void setNull()
        {
            builder.setNull(columnIndex);
        }

        public void setString(String value)
        {
            builder.setString(columnIndex, value);
        }
    }
}
