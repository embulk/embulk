package org.quickload.record;

public class StringType
        extends AbstractType
{
    public static final StringType STRING = new StringType();

    private StringType()
    {
    }

    public String getName()
    {
        return "string";
    }

    @Override
    public Class<?> getJavaType()
    {
        return String.class;
    }

    @Override
    public byte getFixedStorageSize()
    {
        return Type.VARIABLE_LENGTH_INDEX_SIZE;
    }

    static String getStringValue(RecordCursor cursor, int columnIndex)
    {
        int intData = cursor.getInt(columnIndex);
        // TODO serialization mode?
        return cursor.getStringReference(intData);
    }

    static void setStringValue(PageBuilder builder, int columnIndex, String value)
    {
        if (value == null) {
            // this is unnecessary check by design but exists for buggy plugins
            builder.setNull(columnIndex);
        } else {
            // TODO serialization mode?
            int index = builder.addStringReference(value);
            builder.setInt(columnIndex, index);
        }
    }

    @Override
    public String getString(RecordCursor cursor, int columnIndex)
    {
        return getStringValue(cursor, columnIndex);
    }

    @Override
    public void setString(PageBuilder builder, int columnIndex, String value)
    {
        setStringValue(builder, columnIndex, value);
    }

    @Override
    public void consume(RecordCursor cursor, RecordConsumer consumer, Column column)
    {
        if (cursor.isNull(column.getIndex())) {
            consumer.setNull(column);
        } else {
            consumer.setString(column, getStringValue(cursor, column.getIndex()));
        }
    }

    @Override
    public void produce(PageBuilder builder, RecordProducer producer, Column column)
    {
        producer.setString(column, new Setter(builder, column.getIndex()));
    }

    public static class Setter
    {
        private final PageBuilder builder;
        private final int columnIndex;

        Setter(PageBuilder builder, int columnIndex)
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
            setStringValue(builder, columnIndex, value);
        }
    }
}
