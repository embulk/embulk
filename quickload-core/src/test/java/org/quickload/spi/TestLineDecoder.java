package org.quickload.spi;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import org.quickload.buffer.Buffer;
import org.quickload.TestRuntimeModule;
import org.quickload.TestUtilityModule;
import org.quickload.record.RandomSchemaGenerator;
import org.quickload.record.RandomRecordGenerator;

public class TestLineDecoder
{
    // TODO use loadConfig to instantiate LineDecoderTask
    public static class TestTask
            implements LineDecoderTask
    {
        private final String charset;
        private final Newline newline;

        public TestTask(String charset, Newline newline)
        {
            this.charset = charset;
            this.newline = newline;
        }

        @Override
        public String getCharset()
        {
            return charset;
        }

        @Override
        public Newline getNewline()
        {
            return newline;
        }

        @Override
        public void validate()
        {
        }
    }

    private static LineDecoder newDecoder(String charsetName, Newline newline, List<Buffer> buffers)
    {
        return new LineDecoder(buffers, new TestTask(charsetName, newline));
    }

    private static List<String> doDecode(String charsetName, Newline newline, List<Buffer> buffers)
    {
        return ImmutableList.copyOf(newDecoder(charsetName, newline, buffers));
    }

    private static List<Buffer> bufferList(String charsetName, String... sources) throws UnsupportedCharsetException
    {
        Charset charset = Charset.forName(charsetName);

        List<Buffer> buffers = new ArrayList<Buffer>();
        for (String source : sources) {
            ByteBuffer buffer = charset.encode(source);
            buffers.add(Buffer.wrap(buffer.array(), buffer.limit()));
        }

        return buffers;
    }

    @Test
    public void testDecodeBasicAscii() throws Exception
    {
        List<String> decoded = doDecode("utf-8", Newline.LF,
                bufferList("utf-8", "test1\ntest2\ntest3\n"));
        assertEquals(Arrays.asList("test1", "test2", "test3"), decoded);
    }

    @Test
    public void testDecodeBasicAsciiCRLF() throws Exception
    {
        List<String> decoded = doDecode("utf-8", Newline.CRLF,
                bufferList("utf-8", "test1\r\ntest2\r\ntest3\r\n"));
        assertEquals(Arrays.asList("test1", "test2", "test3"), decoded);
    }

    @Test
    public void testDecodeBasicAsciiTail() throws Exception
    {
        List<String> decoded = doDecode("utf-8", Newline.LF,
                bufferList("utf-8", "test1"));
        assertEquals(Arrays.asList("test1"), decoded);
    }

    @Test
    public void testDecodeChunksLF() throws Exception
    {
        List<String> decoded = doDecode("utf-8", Newline.LF,
                bufferList("utf-8", "t", "1", "\n", "t", "2"));
        assertEquals(Arrays.asList("t1", "t2"), decoded);
    }

    @Test
    public void testDecodeChunksCRLF() throws Exception
    {
        List<String> decoded = doDecode("utf-8", Newline.CRLF,
                bufferList("utf-8", "t", "1", "\r\n", "t", "2", "\r", "\n", "t3"));
        assertEquals(Arrays.asList("t1", "t2", "t3"), decoded);
    }

    // TODO test multibytes
}
