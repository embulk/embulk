package org.quickload.spi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.junit.Rule;
import java.nio.charset.Charset;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.TestRuntimeBinder;

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
