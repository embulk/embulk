package org.embulk.spi.time;

import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Exec;
import org.embulk.EmbulkTestRuntime;

public class TestTimestampFormatterParserDeprecated
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
            .set("time_format", "%Y-%m-%d %H:%M:%S.%6N %z");
        FormatterTestTask task = config.loadConfig(FormatterTestTask.class);

        TimestampFormatter formatter = task.getTimeFormat().newFormatter(task);
        assertEquals("2014-11-19 02:46:29.123456 +0000", formatter.format(Timestamp.ofEpochSecond(1416365189, 123456*1000)));
    }

    @Test
    public void testSimpleParse() throws Exception
    {
        ConfigSource config = Exec.newConfigSource()
            .set("time_format", "%Y-%m-%d %H:%M:%S.%N %z");
        ParserTestTask task = config.loadConfig(ParserTestTask.class);

        TimestampParser parser = task.getTimeFormat().newParser(task);
        assertEquals(Timestamp.ofEpochSecond(1416365189, 123456*1000), parser.parse("2014-11-19 02:46:29.123456 +00:00"));
    }

    @Test
    public void testUnixtimeFormat() throws Exception
    {
        ConfigSource config = Exec.newConfigSource()
            .set("time_format", "%s");

        FormatterTestTask ftask = config.loadConfig(FormatterTestTask.class);
        TimestampFormatter formatter = ftask.getTimeFormat().newFormatter(ftask);
        assertEquals("1416365189", formatter.format(Timestamp.ofEpochSecond(1416365189)));

        ParserTestTask ptask = config.loadConfig(ParserTestTask.class);
        TimestampParser parser = ptask.getTimeFormat().newParser(ptask);
        assertEquals(Timestamp.ofEpochSecond(1416365189), parser.parse("1416365189"));
    }
}
