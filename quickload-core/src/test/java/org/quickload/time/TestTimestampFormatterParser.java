package org.quickload.time;

import org.junit.Rule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.quickload.TestRuntimeBinder;
import org.quickload.spi.ExecTask;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;

public class TestTimestampFormatterParser
{
    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    private interface FormatterTestTask
            extends TimestampFormatterTask
    {
        @Config("time_format")
        public TimestampFormatConfig getTimeFormat();
    }

    private interface ParserTestTask
            extends TimestampParserTask
    {
        @Config("time_format")
        public TimestampFormatConfig getTimeFormat();
    }

    protected ExecTask exec;

    @Before
    public void setup()
    {
        exec = binder.newExecTask();
    }

    @Test
    public void testSimpleFormat() throws Exception
    {
        ConfigSource config = new ConfigSource()
            .setString("time_format", "%Y-%m-%d %H:%M:%S %Z");
        FormatterTestTask task = exec.loadConfig(config, FormatterTestTask.class);

        TimestampFormatter formatter = task.getTimeFormat().newFormatter(task);
        assertEquals("2014-11-19 02:46:29 UTC", formatter.format(Timestamp.ofEpochSecond(1416365189)));
    }

    @Test
    public void testSimpleParse() throws Exception
    {
        ConfigSource config = new ConfigSource()
            .setString("time_format", "%Y-%m-%d %H:%M:%S %Z");
        ParserTestTask task = exec.loadConfig(config, ParserTestTask.class);

        TimestampParser parser = task.getTimeFormat().newParser(task);
        assertEquals(Timestamp.ofEpochSecond(1416365189), parser.parse("2014-11-19 02:46:29 UTC"));
    }
}
