package org.quickload.record;

public class DoubleType
        extends AbstractType
{
    public static final DoubleType DOUBLE = new DoubleType();

    private DoubleType()
    {
    }

    @Override
    public Class<?> getJavaType()
    {
        return double.class;
    }

    @Override
    public byte getFixedStorageSize()
    {
        return (byte) 8;
    }

    @Override
    public double getDouble(RecordCursor cursor, int columnIndex)
    {
        return cursor.getDouble(columnIndex);
    }

    @Override
    public void setDouble(RecordBuilder builder, int columnIndex, double value)
    {
        builder.setDouble(columnIndex, value);
    }

    @Override
    public void consume(RecordCursor cursor, RecordConsumer consumer, Column column)
    {
        if (cursor.isNull(column.getIndex())) {
            consumer.setNull(column);
        } else {
            consumer.setDouble(column, cursor.getDouble(column.getIndex()));
        }
    }

    @Override
    public void produce(RecordBuilder builder, RecordProducer producer, Column column)
    {
        producer.setDouble(column, new Setter(builder, column.getIndex()));
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

        public void setDouble(double value)
        {
            builder.setDouble(columnIndex, value);
        }
    }
}
