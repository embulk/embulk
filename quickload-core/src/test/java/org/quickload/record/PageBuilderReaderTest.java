package org.quickload.record;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.quickload.channel.PageChannel;
import org.quickload.exec.BufferManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PageBuilderReaderTest
{
    private final int minCapacity = 128*1024;

    private Random rand = new Random();

    protected BufferManager bufferManager;
    protected PageChannel channel;
    protected PageBuilder builder;
    protected PageReader reader;

    public void createResources(Schema schema) throws Exception
    {
        bufferManager = new BufferManager();
        channel = new PageChannel(minCapacity);
        builder = new PageBuilder(bufferManager, schema, channel.getOutput());
        reader = new PageReader(schema);
    }

    private Schema generateSchema() throws Exception
    {
        int size = rand.nextInt(10) + 1;
        List<Column> columns = new ArrayList<Column>(size);
        for (int i = 0; i < size; i++) {
            columns.add(generateColumn(i));
        }

        System.out.println(" Schema settings:");
        for (Column col : columns) {
            System.out.println("   " + col.toString());
        }
        System.out.println("");
        return new Schema(columns);
    }

    private Column generateColumn(int index) throws Exception
    {
        return new Column(index, "c" + index, generateType());
    }

    private Type generateType() throws Exception
    {
        int index = rand.nextInt(3);
        if (index == 0) {
            return LongType.LONG;
        } else if (index == 1) {
            return DoubleType.DOUBLE;
        } else if (index == 2) {
            return StringType.STRING;
        } else {
            throw new RuntimeException();
        }
    }

    private List<Object[]> generateRecords(Schema schema)
    {
        int size = rand.nextInt(10);
        List<Object[]> records = new ArrayList<Object[]>(size);
        List<Column> columns = schema.getColumns();
        for (int i = 0; i < size; i++) {
            Object[] record = new Object[columns.size()];
            for (int j = 0; j < record.length; j++) {
                record[j] = generateRecord(columns.get(j));
            }
            prettyPrint(record);
            records.add(record);
        }
        return records;
    }

    private Object generateRecord(Column column)
    {
        Type type = column.getType();
        if (type.equals(LongType.LONG)) {
            return rand.nextLong();
        } else if (type.equals(DoubleType.DOUBLE)) {
            return rand.nextDouble();
        } else if (type.equals(StringType.STRING)) {
            return "muga"; // TODO
        } else {
            throw new RuntimeException();
        }
    }

    private void prettyPrint(Object[] record)
    {
        for (int i = 0; i < record.length; i++)
        {
            System.out.print(record[i] + " ");
        }
        System.out.println();
    }

    @After
    public void destroyResources() throws Exception
    {
        channel.close();
    }

    @Test
    public void testRandomData() throws Exception {
        Schema schema = generateSchema();
        createResources(schema);
        final List<Object[]> expected = generateRecords(schema);

        for (final Object[] record : expected) {
            schema.produce(builder, new RecordProducer()
            {
                @Override
                public void setLong(Column column, LongType.Setter setter)
                {
                    setter.setLong((Long) record[column.getIndex()]);
                }

                @Override
                public void setDouble(Column column, DoubleType.Setter setter)
                {
                    setter.setDouble((Double) record[column.getIndex()]);
                }

                @Override
                public void setString(Column column, StringType.Setter setter)
                {
                    setter.setString((String) record[column.getIndex()]);
                }
            });
            builder.addRecord();
        }
        builder.flush();
        channel.completeProducer();

        List<Object[]> actual = new ArrayList<Object[]>();
        for (Page page : channel.getInput()) {
            try (RecordCursor cursor = reader.cursor(page)) {
                while (cursor.next()) {
                    final Object[] record = new Object[schema.getColumns().size()];
                    schema.consume(cursor, new RecordConsumer()
                    {
                        @Override
                        public void setNull(Column column)
                        {
                            // TODO
                        }

                        @Override
                        public void setLong(Column column, long value)
                        {
                            record[column.getIndex()] = value;
                        }

                        @Override
                        public void setDouble(Column column, double value)
                        {
                            record[column.getIndex()] = value;
                        }

                        @Override
                        public void setString(Column column, String value)
                        {
                            record[column.getIndex()] = value;
                        }
                    });
                    prettyPrint(record);
                    actual.add(record);
                }
            }
        }
        channel.completeConsumer();

        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected .size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i));
        }
    }

}
