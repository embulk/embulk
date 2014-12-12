package org.embulk.spi;

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
import org.embulk.TestRuntimeBinder;
import org.embulk.buffer.Buffer;
import org.embulk.channel.BufferChannel;
import org.embulk.channel.FileBufferInput;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.exec.BufferManager;

public class TestTextGuessPlugin
{
    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    private static class TestGuessPlugin extends TextGuessPlugin
    {
        @Override
        public NextConfig guessText(ExecTask exec, ConfigSource config,
                String text)
        {
            return new NextConfig().setString("text", text);
        }

    }

    @Test
    public void testGuess() throws Exception
    {
        String src = "abcdef\rghijk\nlmnop\r\nqrstu";
        TestGuessPlugin plugin = new TestGuessPlugin();

        ExecTask exec = binder.newExecTask();
        ConfigSource config = new ConfigSource().setString("charset",
                "iso8859_1").setString("newline", "LF");

        NextConfig nextConfig = plugin.guess(exec, config,
                Buffer.wrap(src.getBytes("UTF-8")));
        assertEquals(src, nextConfig.getString("text"));
    }
}
