package org.embulk.spi.util;

import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
        assertEquals(StandardCharsets.UTF_8, task.getCharset());
        assertEquals(Newline.CRLF, task.getNewline());
    }

    @Test
    public void testLoadConfig()
    {
        ConfigSource config = Exec.newConfigSource()
            .set("charset", "utf-16")
            .set("newline", "CRLF");
        LineDecoder.DecoderTask task = config.loadConfig(LineDecoder.DecoderTask.class);
        assertEquals(StandardCharsets.UTF_16, task.getCharset());
        assertEquals(Newline.CRLF, task.getNewline());
    }

    private static LineDecoder.DecoderTask getExampleConfig(Charset charset, Newline newline)
    {
        ConfigSource config = Exec.newConfigSource()
            .set("charset", charset)
            .set("newline", newline);
        return config.loadConfig(LineDecoder.DecoderTask.class);
    }

    private static LineDecoder newDecoder(Charset charset, Newline newline, List<Buffer> buffers)
    {
        ListFileInput input = new ListFileInput(ImmutableList.of(buffers));
        return new LineDecoder(input, getExampleConfig(charset, newline));
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

    private static List<Buffer> bufferList(Charset charset, String... sources) throws UnsupportedCharsetException
    {
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
                StandardCharsets.UTF_8, Newline.LF,
                bufferList(StandardCharsets.UTF_8, "test1\ntest2\ntest3\n"));
        assertEquals(ImmutableList.of("test1", "test2", "test3"), decoded);
    }

    @Test
    public void testDecodeBasicAsciiCRLF() throws Exception
    {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.CRLF,
                bufferList(StandardCharsets.UTF_8, "test1\r\ntest2\r\ntest3\r\n"));
        assertEquals(ImmutableList.of("test1", "test2", "test3"), decoded);
    }

    @Test
    public void testDecodeBasicAsciiTail() throws Exception
    {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.LF,
                bufferList(StandardCharsets.UTF_8, "test1"));
        assertEquals(ImmutableList.of("test1"), decoded);
    }

    @Test
    public void testDecodeChunksLF() throws Exception
    {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.LF,
                bufferList(StandardCharsets.UTF_8, "t", "1", "\n", "t", "2"));
        assertEquals(ImmutableList.of("t1", "t2"), decoded);
    }

    @Test
    public void testDecodeChunksCRLF() throws Exception
    {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.CRLF,
                bufferList(StandardCharsets.UTF_8, "t", "1", "\r\n", "t", "2", "\r", "\n", "t3"));
        assertEquals(ImmutableList.of("t1", "t2", "t3"), decoded);
    }

    @Test
    public void testDecodeBasicUTF8() throws Exception
    {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.LF,
                bufferList(StandardCharsets.UTF_8, "てすと1\nテスト2\nてすと3\n"));
        assertEquals(ImmutableList.of("てすと1", "テスト2", "てすと3"), decoded);
    }

    @Test
    public void testDecodeBasicUTF8Tail() throws Exception
    {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.LF,
                bufferList(StandardCharsets.UTF_8, "てすと1"));
        assertEquals(ImmutableList.of("てすと1"), decoded);
    }

    @Test
    public void testDecodeChunksUTF8LF() throws Exception
    {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.LF,
                bufferList(StandardCharsets.UTF_8, "て", "1", "\n", "す", "2"));
        assertEquals(ImmutableList.of("て1", "す2"), decoded);
    }

    @Test
    public void testDecodeChunksUTF8CRLF() throws Exception
    {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.CRLF,
                bufferList(StandardCharsets.UTF_8, "て", "1", "\r\n", "す", "2", "\r", "\n", "と3"));
        assertEquals(ImmutableList.of("て1", "す2", "と3"), decoded);
    }

    @Test
    public void testDecodeBasicUTF16LE() throws Exception
    {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_16LE, Newline.LF,
                bufferList(StandardCharsets.UTF_16LE, "てすと1\nテスト2\nてすと3\n"));
        assertEquals(ImmutableList.of("てすと1", "テスト2", "てすと3"), decoded);
    }

    @Test
    public void testDecodeBasicUTF16LETail() throws Exception
    {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_16LE, Newline.LF,
                bufferList(StandardCharsets.UTF_16LE, "てすと1"));
        assertEquals(ImmutableList.of("てすと1"), decoded);
    }

    @Test
    public void testDecodeChunksUTF16LELF() throws Exception
    {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_16LE, Newline.LF,
                bufferList(StandardCharsets.UTF_16LE, "て", "1", "\n", "す", "2"));
        assertEquals(ImmutableList.of("て1", "す2"), decoded);
    }

    @Test
    public void testDecodeChunksUTF16LECRLF() throws Exception
    {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_16LE, Newline.CRLF,
                bufferList(StandardCharsets.UTF_16LE, "て", "1", "\r\n", "す", "2", "\r", "\n", "と3"));
        assertEquals(ImmutableList.of("て1", "す2", "と3"), decoded);
    }

    @Test
    public void testDecodeBasicMS932() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("ms932"), Newline.LF,
                bufferList(Charset.forName("ms932"), "てすと1\nテスト2\nてすと3\n"));
        assertEquals(ImmutableList.of("てすと1", "テスト2", "てすと3"), decoded);
    }

    @Test
    public void testDecodeBasicMS932Tail() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("ms932"), Newline.LF,
                bufferList(Charset.forName("ms932"), "てすと1"));
        assertEquals(ImmutableList.of("てすと1"), decoded);
    }

    @Test
    public void testDecodeChunksMS932LF() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("ms932"), Newline.LF,
                bufferList(Charset.forName("ms932"), "て", "1", "\n", "す", "2"));
        assertEquals(ImmutableList.of("て1", "す2"), decoded);
    }

    @Test
    public void testDecodeChunksMS932CRLF() throws Exception
    {
        List<String> decoded = doDecode(
                Charset.forName("ms932"), Newline.CRLF,
                bufferList(Charset.forName("ms932"), "て", "1", "\r\n", "す", "2", "\r", "\n", "と3"));
        assertEquals(ImmutableList.of("て1", "す2", "と3"), decoded);
    }
}
