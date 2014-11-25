package org.quickload.record;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.quickload.TestRuntimeBinder;
import org.quickload.TestUtilityModule;
import org.quickload.time.Timestamp;
import org.quickload.buffer.Buffer;
import org.quickload.channel.PageChannel;
import org.quickload.exec.BufferManager;
import org.quickload.exec.ExecModule;
import static org.quickload.record.PageTestUtils.newSchema;
import static org.quickload.record.PageTestUtils.newColumn;
import static org.quickload.record.Types.BOOLEAN;
import static org.quickload.record.Types.LONG;
import static org.quickload.record.Types.DOUBLE;
import static org.quickload.record.Types.STRING;
import static org.quickload.record.Types.TIMESTAMP;

public class TestPageBuilderReader
{
    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    private PageAllocator pageAllocator;
    private PageChannel channel;

    private PageReader reader;
    private PageBuilder builder;

    @Before
    public void setup()
    {
        this.pageAllocator = binder.getInstance(BufferManager.class);
        this.channel = new PageChannel(Integer.MAX_VALUE);
    }

    @After
    public void destroy()
    {
        if (reader != null) {
            reader.close();
            reader = null;
        }
        if (builder != null) {
            builder.close();
            builder = null;
        }
    }

    @Test
    public void testBoolean()
    {
        check(newSchema(newColumn("col1", BOOLEAN)),
                false, true, true);
    }

    @Test
    public void testLong()
    {
        check(newSchema(newColumn("col1", LONG)),
                1L, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @Test
    public void testDouble()
    {
        check(newSchema(newColumn("col1", DOUBLE)),
                8.1, 3.141592, 4.3);
    }

    @Test
    public void testUniqueStrings()
    {
        check(newSchema(newColumn("col1", STRING)),
                "test1", "test2", "test0");
    }

    @Test
    public void testDuplicateStrings()
    {
        check(newSchema(newColumn("col1", STRING)),
                "test1", "test1", "test1");
    }

    @Test
    public void testDuplicateStringsMultiColumns()
    {
        check(newSchema(newColumn("col1", STRING), newColumn("col1", STRING)),
                "test2", "test1",
                "test1", "test2",
                "test2", "test0",
                "test1", "test1");
    }

    @Test
    public void testTimestamp()
    {
        check(newSchema(newColumn("col1", TIMESTAMP)),
                Timestamp.ofEpochMilli(0),
                Timestamp.ofEpochMilli(10));
    }

    @Test
    public void testNull()
    {
        check(newSchema(
                    newColumn("col3", DOUBLE),
                    newColumn("col1", STRING),
                    newColumn("col3", LONG),
                    newColumn("col3", BOOLEAN),
                    newColumn("col2", TIMESTAMP)),
                null, null, null, null, null,
                8122.0, "val1", 3L, false, Timestamp.ofEpochMilli(0),
                null, null, null, null, null);
    }

    @Test
    public void testMixedTypes()
    {
        check(newSchema(
                    newColumn("col3", DOUBLE),
                    newColumn("col1", STRING),
                    newColumn("col3", LONG),
                    newColumn("col3", BOOLEAN),
                    newColumn("col2", TIMESTAMP)),
                8122.0, "val1", 3L, false, Timestamp.ofEpochMilli(0),
                140.15, "val2", Long.MAX_VALUE, true, Timestamp.ofEpochMilli(10));
    }

    private void check(Schema schema, Object... objects)
    {
        buildPage(schema, objects);
        checkPage(schema, objects);
    }

    private void buildPage(Schema schema, final Object... objects)
    {
        this.builder = new PageBuilder(pageAllocator, schema, channel.getOutput());
        final AtomicInteger i = new AtomicInteger(0);
        while (i.get() < objects.length) {
            builder.addRecord(new RecordWriter() {
                public void writeBoolean(Column column, BooleanWriter writer)
                {
                    if (objects[i.get()] == null) {
                        writer.writeNull();
                    } else {
                        writer.write((boolean) objects[i.get()]);
                    }
                    i.getAndIncrement();
                }

                public void writeLong(Column column, LongWriter writer)
                {
                    if (objects[i.get()] == null) {
                        writer.writeNull();
                    } else {
                        writer.write((long) objects[i.get()]);
                    }
                    i.getAndIncrement();
                }

                public void writeDouble(Column column, DoubleWriter writer)
                {
                    if (objects[i.get()] == null) {
                        writer.writeNull();
                    } else {
                        writer.write((double) objects[i.get()]);
                    }
                    i.getAndIncrement();
                }

                public void writeString(Column column, StringWriter writer)
                {
                    if (objects[i.get()] == null) {
                        writer.writeNull();
                    } else {
                        writer.write((String) objects[i.get()]);
                    }
                    i.getAndIncrement();
                }

                public void writeTimestamp(Column column, TimestampWriter writer)
                {
                    if (objects[i.get()] == null) {
                        writer.writeNull();
                    } else {
                        writer.write((Timestamp) objects[i.get()]);
                    }
                    i.getAndIncrement();
                }
            });
        }
        builder.close();
        channel.completeProducer();
    }

    private void checkPage(Schema schema, final Object... objects)
    {
        this.reader = new PageReader(schema, channel.getInput());
        final AtomicInteger i = new AtomicInteger(0);
        while (reader.nextRecord()) {
            reader.visitColumns(new RecordReader() {
                public void readNull(Column column)
                {
                    assertNull(objects[i.getAndIncrement()]);
                }

                public void readBoolean(Column column, boolean value)
                {
                    assertEquals(objects[i.getAndIncrement()], value);
                }

                public void readLong(Column column, long value)
                {
                    assertEquals(objects[i.getAndIncrement()], value);
                }

                public void readDouble(Column column, double value)
                {
                    assertNotNull(objects[i.get()]);
                    assertEquals((double) objects[i.getAndIncrement()], value, 0.00001);
                }

                public void readString(Column column, String value)
                {
                    assertEquals(objects[i.getAndIncrement()], value);
                }

                public void readTimestamp(Column column, Timestamp value)
                {
                    assertEquals(objects[i.getAndIncrement()], value);
                }
            });
        }
        assertEquals(objects.length, i.get());
    }

    @Test
    public void testEmptySchema()
    {
        this.builder = new PageBuilder(pageAllocator, newSchema(), channel.getOutput());
        builder.addRecord();
        builder.addRecord();
        builder.close();
        channel.completeProducer();
        this.reader = new PageReader(newSchema(), channel.getInput());
        assertTrue(reader.nextRecord());
        assertTrue(reader.nextRecord());
        assertFalse(reader.nextRecord());
    }

    @Test
    public void testRenewPage()
    {
        this.pageAllocator = new PageAllocator() {
            public Page allocatePage(int minimumCapacity)
            {
                return Page.allocate(minimumCapacity);
            }
        };
        check(newSchema(newColumn("col1", LONG)),
                0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
    }

    @Test
    public void testRenewPageWithStrings()
    {
        this.pageAllocator = new PageAllocator() {
            public Page allocatePage(int minimumCapacity)
            {
                return Page.allocate(minimumCapacity + 3);
            }
        };
        check(newSchema(
                    newColumn("col1", LONG),
                    newColumn("col1", STRING)),
                0L, "record0",
                1L, "record1",
                3L, "record3");
    }

    @Test
    public void testRepeatableClose()
    {
        this.builder = new PageBuilder(pageAllocator, newSchema(newColumn("col1", STRING)), channel.getOutput());
        builder.close();
        builder.close();
        channel.completeProducer();
        for (Page page : channel.getInput()) {
            fail();
        }
    }

    @Test
    public void testRepeatableFlush()
    {
        this.builder = new PageBuilder(pageAllocator, newSchema(newColumn("col1", STRING)), channel.getOutput());
        builder.flush();
        builder.flush();
        channel.completeProducer();
        for (Page page : channel.getInput()) {
            fail();
        }
    }
}
