package org.embulk.spi;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.embulk.buffer.Buffer;

import com.google.common.collect.ImmutableList;

public class TestLineDecoder
{
    // TODO use loadConfig to instantiate LineDecoderTask
    public static class TestTask
            implements LineDecoderTask
    {
        private final Charset charset;
        private final Newline newline;

        public TestTask(Charset charset, Newline newline)
        {
            this.charset = charset;
            this.newline = newline;
        }

        @Override
        public Charset getCharset()
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

    private static LineDecoder newDecoder(Charset charset, Newline newline, List<Buffer> buffers)
    {
        return new LineDecoder(buffers, new TestTask(charset, newline));
    }

    private static List<String> doDecode(Charset charset, Newline newline, List<Buffer> buffers)
    {
        return ImmutableList.copyOf(newDecoder(charset, newline, buffers));
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
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.LF,
                bufferList("utf-8", "test1\ntest2\ntest3\n"));
        assertEquals(Arrays.asList("test1", "test2", "test3"), decoded);
    }

    @Test
    public void testDecodeBasicAsciiCRLF() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.CRLF,
                bufferList("utf-8", "test1\r\ntest2\r\ntest3\r\n"));
        assertEquals(Arrays.asList("test1", "test2", "test3"), decoded);
    }

    @Test
    public void testDecodeBasicAsciiTail() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.LF,
                bufferList("utf-8", "test1"));
        assertEquals(Arrays.asList("test1"), decoded);
    }

    @Test
    public void testDecodeChunksLF() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.LF,
                bufferList("utf-8", "t", "1", "\n", "t", "2"));
        assertEquals(Arrays.asList("t1", "t2"), decoded);
    }

    @Test
    public void testDecodeChunksCRLF() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.CRLF,
                bufferList("utf-8", "t", "1", "\r\n", "t", "2", "\r", "\n", "t3"));
        assertEquals(Arrays.asList("t1", "t2", "t3"), decoded);
    }

    // TODO test multibytes
}
