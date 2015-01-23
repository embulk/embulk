package org.embulk.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Rule;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import org.embulk.EmbulkTestRuntime;
import org.embulk.spi.util.InputStreamFileInput;

public class TestInputStreamFileInput
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Test
    public void testSingleProvider() throws IOException
    {
        InputStreamFileInput subject = new InputStreamFileInput(
                runtime.getBufferAllocator(),
                provider(new ByteArrayInputStream("abcdef".getBytes("UTF-8"))));
        assertEquals(true, subject.nextFile());
        assertEquals("abcdef", bufferToString(subject.poll()));
        assertEquals(null, subject.poll());
        subject.close();
    }

    @Test
    public void testMultipleProvider() throws IOException
    {
        InputStreamFileInput subject = new InputStreamFileInput(
                runtime.getBufferAllocator(), provider(
                        new ByteArrayInputStream("abcdef".getBytes("UTF-8")),
                        new ByteArrayInputStream("ghijkl".getBytes("UTF-8")),
                        new ByteArrayInputStream("mnopqr".getBytes("UTF-8"))));
        assertEquals(true, subject.nextFile());
        assertEquals("abcdef", bufferToString(subject.poll()));
        assertEquals(true, subject.nextFile());
        assertEquals("ghijkl", bufferToString(subject.poll()));
        assertEquals(true, subject.nextFile());
        assertEquals("mnopqr", bufferToString(subject.poll()));
        subject.close();
    }

    @Test
    public void testEmptyStream() throws IOException
    {
        InputStreamFileInput subject = new InputStreamFileInput(
                runtime.getBufferAllocator(),
                provider(new ByteArrayInputStream(new byte[0])));
        assertEquals(true, subject.nextFile());
        assertEquals(null, subject.poll());
        subject.close();
    }

    @Test
    public void testPollFirstException() throws IOException
    {
        InputStreamFileInput subject = new InputStreamFileInput(
                runtime.getBufferAllocator(),
                provider(new ByteArrayInputStream("abcdef".getBytes("UTF-8"))));
        try {
            subject.poll();
            fail();
        } catch (IllegalStateException ile) {
            // OK
        }
        subject.close();
    }

    @Test
    public void testEmptyProvider() throws IOException
    {
        InputStreamFileInput subject = new InputStreamFileInput(
                runtime.getBufferAllocator(), provider(new InputStream[0]));
        assertEquals(false, subject.nextFile());
        subject.close();
    }

    @Test
    public void testProviderOpenNextException()
    {
        InputStreamFileInput subject = new InputStreamFileInput(
                runtime.getBufferAllocator(),
                new InputStreamFileInput.Provider()
                {
                    @Override
                    public InputStream openNext() throws IOException
                    {
                        throw new IOException("emulated exception");
                    }

                    @Override
                    public void close() throws IOException
                    {
                    }
                });

        try {
            subject.nextFile();
            fail();
        } catch (RuntimeException re) {
            // OK
        }
        subject.close();
    }

    @Test
    public void testProviderCloseException()
    {
        InputStreamFileInput subject = new InputStreamFileInput(
                runtime.getBufferAllocator(),
                new InputStreamFileInput.Provider()
                {
                    @Override
                    public InputStream openNext() throws IOException
                    {
                        return new ByteArrayInputStream(new byte[0]);
                    }

                    @Override
                    public void close() throws IOException
                    {
                        throw new IOException("emulated exception");
                    }
                });

        try {
            subject.close();
            fail();
        } catch (RuntimeException re) {
            // OK
        }
    }

    @Test
    public void testInputStreamReadException()
    {
        InputStreamFileInput subject = new InputStreamFileInput(
                runtime.getBufferAllocator(),
                new InputStreamFileInput.Provider()
                {
                    @Override
                    public InputStream openNext() throws IOException
                    {
                        return new InputStream()
                        {
                            @Override
                            public int read() throws IOException
                            {
                                throw new IOException("emulated exception");
                            }
                        };
                    }

                    @Override
                    public void close() throws IOException
                    {
                    }
                });

        assertEquals(true, subject.nextFile());
        try {
            subject.poll();
            fail();
        } catch (RuntimeException re) {
            // OK
        }
        subject.close();
    }

    private InputStreamFileInput.IteratorProvider provider(
            InputStream... inputStreams) throws IOException
    {
        return new InputStreamFileInput.IteratorProvider(
                ImmutableList.copyOf(inputStreams));
    }

    private String bufferToString(Buffer buffer) throws IOException
    {
        byte[] buf = new byte[buffer.limit()];
        buffer.getBytes(0, buf, 0, buffer.limit());
        return new String(buf, "UTF-8");
    }
}
