package org.embulk.spi.time;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestTimestampFormatterParser {
    @Test
    public void testSimpleFormat() throws Exception {
        final TimestampFormatter formatter = TimestampFormatter.of("%Y-%m-%d %H:%M:%S.%9N %z", "UTC");
        assertEquals("2014-11-19 02:46:29.123456000 +0000", formatter.format(Timestamp.ofEpochSecond(1416365189, 123456 * 1000)));
    }

    @Test
    public void testSimpleParse() throws Exception {
        final TimestampParser parser = TimestampParser.of("%Y-%m-%d %H:%M:%S %z", "UTC");
        assertEquals(Timestamp.ofEpochSecond(1416365189, 0), parser.parse("2014-11-19 02:46:29 +0000"));
    }

    @Test
    public void testUnixtimeFormat() throws Exception {
        final TimestampFormatter formatter = TimestampFormatter.of("%s", "UTC");
        assertEquals("1416365189", formatter.format(Timestamp.ofEpochSecond(1416365189)));

        final TimestampParser parser = TimestampParser.of("%s", "UTC");
        assertEquals(Timestamp.ofEpochSecond(1416365189), parser.parse("1416365189"));
    }

    @Test
    public void testDefaultDate() throws Exception {
        final TimestampParser parser = TimestampParser.of("%H:%M:%S %Z", "UTC", "2016-02-03");
        assertEquals(Timestamp.ofEpochSecond(1454467589, 0), parser.parse("02:46:29 +0000"));
    }
}
