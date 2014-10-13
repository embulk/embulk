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

    @Override
    public long getLong(RecordCursor cursor, int columnIndex)
    {
        return cursor.getLong(columnIndex);
    }

    @Override
    public void setLong(PageBuilder builder, int columnIndex, long value)
    {
        builder.setLong(columnIndex, value);
    }

    @Override
    public void consume(RecordCursor cursor, RecordConsumer consumer, Column column)
    {
        if (cursor.isNull(column.getIndex())) {
            consumer.setNull(column);
        } else {
            consumer.setLong(column, cursor.getLong(column.getIndex()));
        }
    }

    @Override
    public void produce(PageBuilder builder, RecordProducer producer, Column column)
    {
        producer.setLong(column, new Setter(builder, column.getIndex()));
    }

    public static class Setter
    {
        private final PageBuilder builder;
        private final int columnIndex;

        private Setter(PageBuilder builder, int columnIndex)
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
            builder.setLong(columnIndex, value);
        }
    }
}
