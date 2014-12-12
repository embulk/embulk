package org.embulk.record;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.embulk.GuiceJUnitRunner;
import org.embulk.TestUtilityModule;
import org.embulk.channel.PageChannel;
import org.embulk.exec.BufferManager;
import org.embulk.exec.ExecModule;
import org.embulk.time.Timestamp;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ ExecModule.class, TestUtilityModule.class })
public class TestRandomPageBuilderReader
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

    @Before
    public void setup() throws Exception
    {
        channel = new PageChannel(Integer.MAX_VALUE);
        schema = schemaGen.generate(60);
        builder = new PageBuilder(bufferManager, schema, channel.getOutput());
    }

    @After
    public void destroy() throws Exception
    {
        channel.close();
        builder.close();
    }

    @Test
    public void testRandomData() throws Exception {
        final List<Record> expected = ImmutableList.copyOf(recordGen.generate(schema, 5000));

        for (final Record record : expected) {
            builder.addRecord(new RecordWriter() {
                public void writeBoolean(Column column, BooleanWriter writer)
                {
                    writer.write((boolean) record.getObject(column.getIndex()));
                }

                public void writeLong(Column column, LongWriter writer)
                {
                    writer.write((long) record.getObject(column.getIndex()));
                }

                public void writeDouble(Column column, DoubleWriter writer)
                {
                    writer.write((double) record.getObject(column.getIndex()));
                }

                public void writeString(Column column, StringWriter writer)
                {
                    writer.write((String) record.getObject(column.getIndex()));
                }

                public void writeTimestamp(Column column, TimestampWriter writer)
                {
                    writer.write((Timestamp) record.getObject(column.getIndex()));
                }
            });
        }
        builder.flush();
        channel.completeProducer();

        List<Record> actual = new ArrayList<Record>();
        for (Object[] values : Pages.toObjects(schema, channel.getInput())) {
            actual.add(new Record(values));
        }
        channel.completeConsumer();

        assertEquals(expected, actual);
    }


}
