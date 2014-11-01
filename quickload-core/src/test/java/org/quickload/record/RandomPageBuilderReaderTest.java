package org.quickload.record;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quickload.channel.PageChannel;
import org.quickload.exec.BufferManager;

import java.util.ArrayList;
import java.util.List;

import static org.quickload.record.Assert.assertRowsEquals;

public class RandomPageBuilderReaderTest
{
    private final int minCapacity = 128*1024;
    private int schemaSize = 3;
    private int rowSize = 10;

    protected BufferManager bufferManager;
    protected RandomSeedManager randomSeedManager;

    protected PageChannel channel;

    protected Schema schema;
    protected TestRandomRecordGenerator gen;
    protected PageBuilder builder;
    protected PageReader reader;

    public RandomPageBuilderReaderTest() throws Exception
    {
        randomSeedManager = new RandomSeedManager();
        bufferManager = new BufferManager();
    }

    @Before
    public void createResources() throws Exception
    {
        channel = new PageChannel(minCapacity);

        schema = new TestRandomSchemaGenerator(randomSeedManager).generate(schemaSize);
        gen = new TestRandomRecordGenerator(randomSeedManager);
        builder = new PageBuilder(bufferManager, schema, channel.getOutput());
        reader = new PageReader(schema);
    }

    @After
    public void destroyResources() throws Exception
    {
        channel.close();
    }

    @Test
    public void testRandomData() throws Exception {
        final List<Row> expected = ImmutableList.copyOf(gen.generate(schema, rowSize));

        for (final Row record : expected) {
            schema.produce(builder, new RecordProducer()
            {
                @Override
                public void setLong(Column column, LongType.Setter setter)
                {
                    setter.setLong((Long) record.getRecord(column.getIndex()));
                }

                @Override
                public void setDouble(Column column, DoubleType.Setter setter)
                {
                    setter.setDouble((Double) record.getRecord(column.getIndex()));
                }

                @Override
                public void setString(Column column, StringType.Setter setter)
                {
                    setter.setString((String) record.getRecord(column.getIndex()));
                }
            });
            builder.addRecord();
        }
        builder.flush();
        channel.completeProducer();

        List<Row> actual = new ArrayList<Row>();
        for (Page page : channel.getInput()) {
            try (RecordCursor cursor = reader.cursor(page)) {
                while (cursor.next()) {
                    final Object[] row = new Object[schema.getColumns().size()];
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
                            row[column.getIndex()] = value;
                        }

                        @Override
                        public void setDouble(Column column, double value)
                        {
                            row[column.getIndex()] = value;
                        }

                        @Override
                        public void setString(Column column, String value)
                        {
                            row[column.getIndex()] = value;
                        }
                    });
                    actual.add(new Row(row));
                }
            }
        }
        channel.completeConsumer();

        assertRowsEquals(expected, actual);
    }

}
