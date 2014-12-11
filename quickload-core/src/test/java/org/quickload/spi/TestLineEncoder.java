package org.quickload.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.quickload.TestRuntimeBinder;
import org.quickload.buffer.Buffer;
import org.quickload.channel.BufferChannel;
import org.quickload.channel.BufferOutput;
import org.quickload.exec.BufferManager;

import com.google.common.collect.ImmutableList;

public class TestLineEncoder
{
    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    public static class TestTask implements LineEncoderTask
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

    private LineEncoder newEncoder(Charset charset, Newline newline,
            BufferOutput output)
    {
        return new LineEncoder(binder.getInstance(BufferManager.class),
                new TestTask(charset, newline), output);
    }

    @Test
    public void testAddLine() throws Exception
    {
        try (BufferChannel channel = new BufferChannel(1024)) {
            LineEncoder encoder = newEncoder(Charset.forName("utf-8"),
                    Newline.LF, channel.getOutput());
            for (String line : new String[] { "abc", "日本語(Japanese)" }) {
                encoder.addLine(line);
            }
            channel.completeProducer();
            Iterator<Buffer> ite = channel.getInput().iterator();
            assertEquals("abc", bufferToString(ite.next(), "utf-8"));
            assertEquals("\n", bufferToString(ite.next(), "utf-8"));
            assertEquals("日本語(Japanese)", bufferToString(ite.next(), "utf-8"));
            assertEquals("\n", bufferToString(ite.next(), "utf-8"));
            assertFalse(ite.hasNext());
            channel.completeConsumer();
        }
    }

    @Test
    public void testAddTextAddNewLine() throws Exception
    {
        try (BufferChannel channel = new BufferChannel(1024)) {
            LineEncoder encoder = newEncoder(Charset.forName("utf-8"),
                    Newline.LF, channel.getOutput());
            for (String line : new String[] { "abc", "日本語(Japanese)" }) {
                encoder.addText(line);
                encoder.addNewLine();
            }
            channel.completeProducer();
            Iterator<Buffer> ite = channel.getInput().iterator();
            assertEquals("abc", bufferToString(ite.next(), "utf-8"));
            assertEquals("\n", bufferToString(ite.next(), "utf-8"));
            assertEquals("日本語(Japanese)", bufferToString(ite.next(), "utf-8"));
            assertEquals("\n", bufferToString(ite.next(), "utf-8"));
            assertFalse(ite.hasNext());
            channel.completeConsumer();
        }
    }

    @Test
    public void testNewLine() throws Exception
    {
        try (BufferChannel channel = new BufferChannel(1024)) {
            LineEncoder encoder = newEncoder(Charset.forName("utf-8"),
                    Newline.CRLF, channel.getOutput());
            for (String line : new String[] { "abc", "日本語(Japanese)" }) {
                encoder.addLine(line);
            }
            channel.completeProducer();
            Iterator<Buffer> ite = channel.getInput().iterator();
            assertEquals("abc", bufferToString(ite.next(), "utf-8"));
            assertEquals("\r\n", bufferToString(ite.next(), "utf-8"));
            assertEquals("日本語(Japanese)", bufferToString(ite.next(), "utf-8"));
            assertEquals("\r\n", bufferToString(ite.next(), "utf-8"));
            assertFalse(ite.hasNext());
            channel.completeConsumer();
        }
    }

    @Test
    public void testCharset() throws Exception
    {
        try (BufferChannel channel = new BufferChannel(1024)) {
            LineEncoder encoder = newEncoder(Charset.forName("MS932"),
                    Newline.CR, channel.getOutput());
            for (String line : new String[] { "abc", "日本語(Japanese)" }) {
                encoder.addLine(line);
            }
            channel.completeProducer();
            Iterator<Buffer> ite = channel.getInput().iterator();
            assertEquals("abc", bufferToString(ite.next(), "MS932"));
            assertEquals("\r", bufferToString(ite.next(), "MS932"));
            assertEquals("日本語(Japanese)", bufferToString(ite.next(), "MS932"));
            assertEquals("\r", bufferToString(ite.next(), "MS932"));
            assertFalse(ite.hasNext());
            channel.completeConsumer();
        }
    }

    private String bufferToString(Buffer buffer, String charset)
            throws UnsupportedEncodingException
    {
        return new String(buffer.get(), 0, buffer.limit(), charset);
    }
}
