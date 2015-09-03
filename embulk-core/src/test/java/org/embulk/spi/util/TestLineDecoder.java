package org.embulk.spi.util;

import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Exec;
import org.embulk.spi.Buffer;
import org.embulk.spi.util.ListFileInput;
import org.embulk.EmbulkTestRuntime;

public class TestLineDecoder
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Test
    public void testDefaultValues()
    {
        ConfigSource config = Exec.newConfigSource();
        LineDecoder.DecoderTask task = config.loadConfig(LineDecoder.DecoderTask.class);
        assertEquals(Charset.forName("utf-8"), task.getCharset());
        assertEquals(Newline.CRLF, task.getNewline());
    }

    @Test
    public void testLoadConfig()
    {
        ConfigSource config = Exec.newConfigSource()
            .set("charset", "utf-16")
            .set("newline", "CRLF");
        LineDecoder.DecoderTask task = config.loadConfig(LineDecoder.DecoderTask.class);
        assertEquals(Charset.forName("utf-16"), task.getCharset());
        assertEquals(Newline.CRLF, task.getNewline());
    }

    private static LineDecoder.DecoderTask getExampleConfig()
    {
        ConfigSource config = Exec.newConfigSource()
            .set("charset", "utf-8")
            .set("newline", "LF");
        return config.loadConfig(LineDecoder.DecoderTask.class);
    }

    private static LineDecoder newDecoder(Charset charset, Newline newline, List<Buffer> buffers)
    {
        ListFileInput input = new ListFileInput(ImmutableList.of(buffers));
        return new LineDecoder(input, getExampleConfig());
    }

    private static List<String> doDecode(Charset charset, Newline newline, List<Buffer> buffers)
    {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        LineDecoder decoder = newDecoder(charset, newline, buffers);
        decoder.nextFile();
        while (true) {
            String line = decoder.poll();
            if (line == null) {
                break;
            }
            builder.add(line);
        }
        return builder.build();
    }

    private static List<Buffer> bufferList(String charsetName, String... sources) throws UnsupportedCharsetException
    {
        Charset charset = Charset.forName(charsetName);

        List<Buffer> buffers = new ArrayList<Buffer>();
        for (String source : sources) {
            ByteBuffer buffer = charset.encode(source);
            buffers.add(Buffer.wrap(buffer.array(), 0, buffer.limit()));
        }

        return buffers;
    }

    @Test
    public void testDecodeBasicAscii() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.LF,
                bufferList("utf-8", "test1\ntest2\ntest3\n"));
        assertEquals(ImmutableList.of("test1", "test2", "test3"), decoded);
    }

    @Test
    public void testDecodeBasicAsciiCRLF() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.CRLF,
                bufferList("utf-8", "test1\r\ntest2\r\ntest3\r\n"));
        assertEquals(ImmutableList.of("test1", "test2", "test3"), decoded);
    }

    @Test
    public void testDecodeBasicAsciiTail() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.LF,
                bufferList("utf-8", "test1"));
        assertEquals(ImmutableList.of("test1"), decoded);
    }

    @Test
    public void testDecodeChunksLF() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.LF,
                bufferList("utf-8", "t", "1", "\n", "t", "2"));
        assertEquals(ImmutableList.of("t1", "t2"), decoded);
    }

    @Test
    public void testDecodeChunksCRLF() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.CRLF,
                bufferList("utf-8", "t", "1", "\r\n", "t", "2", "\r", "\n", "t3"));
        assertEquals(ImmutableList.of("t1", "t2", "t3"), decoded);
    }

    @Test
    public void testDecodeBasicMultiByte() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.LF,
                bufferList("utf-8", "てすと1\nテスト2\nてすと3\n"));
        assertEquals(ImmutableList.of("てすと1", "テスト2", "てすと3"), decoded);
    }

    @Test
    public void testDecodeBasicMultiByteTail() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.LF,
                bufferList("utf-8", "てすと1"));
        assertEquals(ImmutableList.of("てすと1"), decoded);
    }

    @Test
    public void testDecodeChunksMultiByteLF() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.LF,
                bufferList("utf-8", "て", "1", "\n", "す", "2"));
        assertEquals(ImmutableList.of("て1", "す2"), decoded);
    }

    @Test
    public void testDecodeChunksMultiByteCRLF() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("utf-8"), Newline.CRLF,
                bufferList("utf-8", "て", "1", "\r\n", "す", "2", "\r", "\n", "と3"));
        assertEquals(ImmutableList.of("て1", "す2", "と3"), decoded);
    }
}
