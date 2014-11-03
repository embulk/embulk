package org.quickload.record;

import com.google.common.collect.ImmutableList;
import com.google.inject.*;
import eu.fabiostrozzi.guicejunitrunner.GuiceJUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.junit.runner.RunWith;
import org.quickload.TestUtilityModule;
import org.quickload.buffer.Buffer;
import org.quickload.channel.PageChannel;
import org.quickload.exec.BufferManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ TestUtilityModule.class })
public class RandomPageBuilderReaderTest
{
    @Inject
    protected BufferManager bufferManager;
    @Inject
    protected RandomSchemaGenerator schemaGen;
    @Inject
    protected RandomRecordGenerator recordGen;

    protected PageChannel channel;
    protected Schema schema;
    protected RandomRecordGenerator gen;
    protected PageBuilder builder;
    protected PageReader reader;

    @Before
    public void createResources() throws Exception
    {
        channel = new PageChannel(64*1024*1024);
        schema = schemaGen.generate(60);
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
        final List<Record> expected = ImmutableList.copyOf(recordGen.generate(schema, 5000));

        for (final Record record : expected) {
            schema.produce(builder, new RecordProducer()
            {
                @Override
                public void setLong(Column column, LongType.Setter setter)
                {
                    setter.setLong((Long) record.getObject(column.getIndex()));
                }

                @Override
                public void setDouble(Column column, DoubleType.Setter setter)
                {
                    setter.setDouble((Double) record.getObject(column.getIndex()));
                }

                @Override
                public void setString(Column column, StringType.Setter setter)
                {
                    setter.setString((String) record.getObject(column.getIndex()));
                }
            });
            builder.addRecord();
        }
        builder.flush();
        channel.completeProducer();

        List<Record> actual = new ArrayList<Record>();
        for (Page page : channel.getInput()) {
            try (RecordCursor cursor = reader.cursor(page)) {
                while (cursor.next()) {
                    final Object[] values = new Object[schema.getColumns().size()];
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
                            values[column.getIndex()] = value;
                        }

                        @Override
                        public void setDouble(Column column, double value)
                        {
                            values[column.getIndex()] = value;
                        }

                        @Override
                        public void setString(Column column, String value)
                        {
                            values[column.getIndex()] = value;
                        }
                    });
                    actual.add(new Record(values));
                }
            }
        }
        channel.completeConsumer();

        assertEquals(expected, actual);
    }
}
