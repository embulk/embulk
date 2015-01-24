package org.embulk.spi.time;

import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Exec;
import org.embulk.EmbulkTestRuntime;

public class TestTimestampFormatterParser
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private interface FormatterTestTask
            extends TimestampFormatter.FormatterTask
    {
        @Config("time_format")
        public TimestampFormat getTimeFormat();
    }

    private interface ParserTestTask
            extends TimestampParser.ParserTask
    {
        @Config("time_format")
        public TimestampFormat getTimeFormat();
    }

    @Test
    public void testSimpleFormat() throws Exception
    {
        ConfigSource config = Exec.newConfigSource()
            .set("time_format", "%Y-%m-%d %H:%M:%S %Z");
        FormatterTestTask task = config.loadConfig(FormatterTestTask.class);

        TimestampFormatter formatter = task.getTimeFormat().newFormatter(task);
        assertEquals("2014-11-19 02:46:29 UTC", formatter.format(Timestamp.ofEpochSecond(1416365189)));
    }

    @Test
    public void testSimpleParse() throws Exception
    {
        ConfigSource config = Exec.newConfigSource()
            .set("time_format", "%Y-%m-%d %H:%M:%S %Z");
        ParserTestTask task = config.loadConfig(ParserTestTask.class);

        TimestampParser parser = task.getTimeFormat().newParser(task);
        assertEquals(Timestamp.ofEpochSecond(1416365189), parser.parse("2014-11-19 02:46:29 UTC"));
    }
}
