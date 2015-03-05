package org.embulk.spi;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.embulk.spi.type.Types;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPageReader {
    Column column;
    Schema schema;
    MockBufferAllocator bufferAllocator;
    MockBuffer fooBuffer;
    MockBuffer barBuffer;
    List<Page> pages;

    @Before
    public void setUp() throws Exception {
        column = new Column(0, "name", Types.STRING);
        schema = PageTestUtils.newSchema(column);
        bufferAllocator = new MockBufferAllocator();
        pages = PageTestUtils.buildPage(bufferAllocator, schema, "foo", "bar");
        fooBuffer = bufferAllocator.getAllocatedBuffers().get(0);
        barBuffer = bufferAllocator.getAllocatedBuffers().get(1);
    }

    @After
    public void tearDown() throws Exception {
        column = null;
        schema = null;
        bufferAllocator = null;
        fooBuffer = null;
        barBuffer = null;
        pages = null;
    }

    @Test
    public void testPageReaderDefaultReleasePageBuffer() {
        PageReader reader = new PageReader(schema);
        for (Page page : pages) {
            reader.setPage(page);
        }
        reader.close();

        assertEquals("Verify foo is not released", 1, fooBuffer.getReleasedCount());
        assertEquals("Verify bar is not released", 1, barBuffer.getReleasedCount());
    }

    @Test
    public void testPageReaderDontReleasePage() {
        PageReader reader = new PageReader(schema, false);
        for (Page page : pages) {
            reader.setPage(page);
        }
        reader.close();

        assertEquals("Verify foo is released", 0, fooBuffer.getReleasedCount());
        assertEquals("Verify bar is released", 0, barBuffer.getReleasedCount());
    }

}
