package org.quickload.record;

public class LongType
        extends AbstractType
{
    public static final LongType LONG = new LongType();

    private LongType()
    {
    }

    public String getName()
    {
        return "long";
    }

    @Override
    public Class<?> getJavaType()
    {
        return long.class;
    }

    @Override
    public byte getFixedStorageSize()
    {
        return (byte) 8;
    }

    static long getLongValue(RecordCursor cursor, int columnIndex)
    {
        return cursor.getPage().getLong(cursor.getFixedLengthPosition(columnIndex));
    }

    static void setLongValue(RecordBuilder builder, int columnIndex, long value)
    {
        builder.getPage().setLong(builder.getFixedLengthPosition(columnIndex), value);
    }

    @Override
    public long getLong(RecordCursor cursor, int columnIndex)
    {
        return getLongValue(cursor, columnIndex);
    }

    @Override
    public void setLong(RecordBuilder builder, int columnIndex, long value)
    {
        setLongValue(builder, columnIndex, value);
    }

    @Override
    public void consume(RecordCursor cursor, RecordConsumer consumer, Column column)
    {
        if (cursor.isNull(column.getIndex())) {
            consumer.setNull(column);
        } else {
            consumer.setLong(column, getLongValue(cursor, column.getIndex()));
        }
    }

    @Override
    public void produce(RecordBuilder builder, RecordProducer producer, Column column)
    {
        producer.setLong(column, new Setter(builder, column.getIndex()));
    }

    public static class Setter
    {
        private final RecordBuilder builder;
        private final int columnIndex;

        private Setter(RecordBuilder builder, int columnIndex)
        {
            this.builder = builder;
            this.columnIndex = columnIndex;
        }

        public void setNull()
        {
            builder.setNull(columnIndex);
        }

        public void setLong(long value)
        {
            setLongValue(builder, columnIndex, value);
        }
    }
}
