package org.embulk.spi.time;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Optional;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.spi.Exec;
import org.junit.Rule;
import org.junit.Test;

public class TestTimestampFormatterParser {
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private interface FormatterTestTask extends Task, TimestampFormatter.Task {}

    private interface ParserTestTask extends Task, TimestampParser.Task {}

    @Test
    public void testSimpleFormat() throws Exception {
        ConfigSource config = Exec.newConfigSource()
                .set("default_timestamp_format", "%Y-%m-%d %H:%M:%S.%9N %z");  // %Z is OS-dependent
        FormatterTestTask task = config.loadConfig(FormatterTestTask.class);

        TimestampFormatter formatter = TimestampFormatter.of(task, Optional.absent());
        assertEquals("2014-11-19 02:46:29.123456000 +0000", formatter.format(Timestamp.ofEpochSecond(1416365189, 123456 * 1000)));
    }

    @Test
    public void testSimpleParse() throws Exception {
        ConfigSource config = Exec.newConfigSource()
                .set("default_timestamp_format", "%Y-%m-%d %H:%M:%S %z");  // %Z is OS-dependent
        ParserTestTask task = config.loadConfig(ParserTestTask.class);

        TimestampParser parser = TimestampParserLegacy.createTimestampParserForTesting(task);
        assertEquals(Timestamp.ofEpochSecond(1416365189, 0), parser.parse("2014-11-19 02:46:29 +0000"));
    }

    @Test
    public void testUnixtimeFormat() throws Exception {
        ConfigSource config = Exec.newConfigSource()
                .set("default_timestamp_format", "%s");

        FormatterTestTask ftask = config.loadConfig(FormatterTestTask.class);
        TimestampFormatter formatter = TimestampFormatter.of(ftask, Optional.absent());
        assertEquals("1416365189", formatter.format(Timestamp.ofEpochSecond(1416365189)));

        ParserTestTask ptask = config.loadConfig(ParserTestTask.class);
        TimestampParser parser = TimestampParserLegacy.createTimestampParserForTesting(ptask);
        assertEquals(Timestamp.ofEpochSecond(1416365189), parser.parse("1416365189"));
    }

    @Test
    public void testDefaultDate() throws Exception {
        ConfigSource config = Exec.newConfigSource()
                .set("default_timestamp_format", "%H:%M:%S %Z")
                .set("default_date", "2016-02-03");

        ParserTestTask ptask = config.loadConfig(ParserTestTask.class);
        TimestampParser parser = TimestampParserLegacy.createTimestampParserForTesting(ptask);
        assertEquals(Timestamp.ofEpochSecond(1454467589, 0), parser.parse("02:46:29 +0000"));
    }
}
