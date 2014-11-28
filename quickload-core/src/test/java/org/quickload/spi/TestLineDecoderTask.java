package org.quickload.spi;

import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;

import org.junit.Rule;
import org.junit.Test;
import org.quickload.TestRuntimeBinder;
import org.quickload.config.ConfigSource;

public class TestLineDecoderTask
{
    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    @Test
    public void testDefaultValues()
    {
        ExecTask exec = binder.newExecTask();
        LineDecoderTask task = exec.loadConfig(new ConfigSource(), LineDecoderTask.class);
        assertEquals(Charset.forName("utf-8"), task.getCharset());
        assertEquals(Newline.CRLF, task.getNewline());
    }

    @Test
    public void testLoadConfig()
    {
        ExecTask exec = binder.newExecTask();
        ConfigSource config = new ConfigSource()
            .setString("charset", "utf-16")
            .setString("newline", "LF");
        LineDecoderTask task = exec.loadConfig(config, LineDecoderTask.class);
        assertEquals(Charset.forName("utf-16"), task.getCharset());
        assertEquals(Newline.LF, task.getNewline());
    }
}
