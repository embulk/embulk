package org.quickload.channel;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.Timeout;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.quickload.buffer.Buffer;
import org.quickload.DurationWatch;
import static org.quickload.channel.TestDataChannel.doLater;
import static org.quickload.channel.TestDataChannel.newBuffer;

public class TestFileBufferChannel
{
    private FileBufferChannel channel;
    private FileBufferInput input;
    private FileBufferOutput output;

    // to make sure methods won't be blocked for ever
    @Rule
    public Timeout globalTimeout = new Timeout(10000);

    @Rule
    public DurationWatch duration = new DurationWatch();

    @Before
    public void setup() throws Exception
    {
        channel = new FileBufferChannel(Integer.MAX_VALUE);
        input = channel.getInput();
        output = channel.getOutput();
    }

    @Test
    public void testAddFileNextFile()
    {
        Iterator<Buffer> ite = input.iterator();

        Buffer b1 = newBuffer();
        Buffer b2 = newBuffer();
        Buffer b3 = newBuffer();

        // file1
        output.add(b1);
        output.add(b2);
        output.addFile();
        // file2
        output.add(b3);
        output.addFile();

        // before file1
        assertFalse(ite.hasNext());

        // file1
        assertTrue(input.nextFile());
        assertTrue(ite.hasNext());
        assertTrue(ite.next() == b1);
        assertTrue(ite.next() == b2);
        assertFalse(ite.hasNext());

        // file2
        assertTrue(input.nextFile());
        assertTrue(ite.hasNext());
        assertTrue(ite.next() == b3);
        assertFalse(ite.hasNext());
    }

    @Test
    public void testGetAddedFileSize()
    {
        Buffer b1 = newBuffer(10);
        Buffer b2 = newBuffer(20);
        Buffer b3 = newBuffer(30);

        assertEquals(0, output.getAddedSize());
        output.add(newBuffer(10));
        assertEquals(10, output.getAddedSize());
        output.add(newBuffer(20));
        assertEquals(30, output.getAddedSize());
        output.addFile();
        assertEquals(0, output.getAddedSize());
        output.add(newBuffer(40));
        assertEquals(40, output.getAddedSize());
    }

    @Test
    public void testNextFileBlocksUntilAdd()
    {
        doLater(new Runnable() {
            public void run()
            {
                output.add(newBuffer());
            }
        });
        assertTrue(input.nextFile());  // this waits for at least one buffer
        assertTrue(duration.runtimeMillis() > 50);
    }

    @Test(expected = IllegalStateException.class)
    public void testDuplicatedNextFileFails()
    {
        output.add(newBuffer());
        assertTrue(input.nextFile());  // skip the first file first
        input.nextFile();  // this throws IllegalStateException because it gets a buffer
    }

    @Test
    public void testCompleteProducerFinishesNextFile()
    {
        doLater(new Runnable() {
            public void run()
            {
                channel.completeProducer();
            }
        });
        assertFalse(input.nextFile());
        assertTrue(duration.runtimeMillis() > 50);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemoveIsUnsupported() throws Exception
    {
        channel.iterator().remove();
    }
}
