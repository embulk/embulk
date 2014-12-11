package org.quickload.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.quickload.TestRuntimeBinder;
import org.quickload.buffer.Buffer;
import org.quickload.channel.BufferChannel;
import org.quickload.channel.FileBufferInput;
import org.quickload.exec.BufferManager;

public class TestBufferPlugins
{
    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    @Test
    public void testTransferInputStream() throws Exception
    {
        InputStream input = new ByteArrayInputStream(
                "1234567890".getBytes("UTF-8"));

        try (BufferChannel bufferChannel = new BufferChannel(1024)) {
            assertEquals(10, BufferPlugins.transferInputStream(
                    binder.getInstance(BufferManager.class), input,
                    bufferChannel.getOutput()));
            bufferChannel.completeProducer();

            Iterator<Buffer> ite = bufferChannel.getInput().iterator();
            assertTrue(ite.hasNext());
            Buffer buffer = ite.next();
            assertEquals("1234567890",
                    new String(buffer.get(), 0, buffer.limit(), "UTF-8"));
            assertFalse(ite.hasNext());
        }
    }

    @Test
    public void testTransferBufferInput() throws Exception
    {
        Buffer[] buffers = new Buffer[] {
                Buffer.wrap("12345".getBytes("UTF-8")),
                Buffer.wrap("".getBytes("UTF-8")),
                Buffer.wrap("67890".getBytes("UTF-8")) };
        OutputStream output = new ByteArrayOutputStream(1024);

        try (MockFileBufferInput fileBufferChannel = new MockFileBufferInput(
                Arrays.asList(buffers))) {

            FileBufferInput input = fileBufferChannel.getInput();
            assertTrue(input.nextFile());
            assertEquals(10, BufferPlugins.transferBufferInput(
                    binder.getInstance(BufferManager.class), input, output));
            assertFalse(input.nextFile());

            assertEquals("1234567890", output.toString());
        }
    }
}