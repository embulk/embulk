package org.embulk.spi.unit;

import org.embulk.spi.time.Timestamp;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestTimestampParam
{
    @Test
    public void testUnitPatterns()
        throws Exception
    {
        assertTimestamp(1471384563000L, "2016-08-16T14:56:03-07:00");
        assertTimestamp(1471384563000L, "2016-08-16T14:56:03-0700");
        assertTimestamp(1471384563000L, "2016-08-16 14:56:03 -07:00");
        assertTimestamp(1471384563000L, "2016-08-16 14:56:03 -0700");

        assertTimestamp(1471359363000L, "2016-08-16T14:56:03+00:00");
        assertTimestamp(1471359363000L, "2016-08-16T14:56:03+0000");
        assertTimestamp(1471359363000L, "2016-08-16T14:56:03Z");
        assertTimestamp(1471359363000L, "2016-08-16 14:56:03 +00:00");
        assertTimestamp(1471359363000L, "2016-08-16 14:56:03 +0000");
        assertTimestamp(1471359363000L, "2016-08-16 14:56:03 Z");
        assertTimestamp(1471359363000L, "2016-08-16 14:56:03 UTC");

        assertInvalid("2016-08-16T14:56:03 -07:00");
        assertInvalid("2016-08-16T14:56:03 -0700");
        assertInvalid("2016-08-16 14:56:03-07:00");
        assertInvalid("2016-08-16 14:56:03-0700");

        assertInvalid("2016-08-16T14:56:03 +00:00");
        assertInvalid("2016-08-16T14:56:03 +0000");
        assertInvalid("2016-08-16T14:56:03 Z");
        assertInvalid("2016-08-16T14:56:03 UTC");
        assertInvalid("2016-08-16 14:56:03+00:00");
        assertInvalid("2016-08-16 14:56:03+0000");
        assertInvalid("2016-08-16 14:56:03Z");
        assertInvalid("2016-08-16 14:56:03UTC");
    }

    @Test
    public void testUnitPatternsWithFracSeconds()
        throws Exception
    {
        assertTimestamp(1471384563031L, "2016-08-16T14:56:03.031-07:00");
        assertTimestamp(1471384563031L, "2016-08-16T14:56:03.031-0700");
        assertTimestamp(1471384563031L, "2016-08-16 14:56:03.031 -07:00");
        assertTimestamp(1471384563031L, "2016-08-16 14:56:03.031 -0700");

        assertTimestamp(1471359363031L, "2016-08-16T14:56:03.031+00:00");
        assertTimestamp(1471359363031L, "2016-08-16T14:56:03.031+0000");
        assertTimestamp(1471359363031L, "2016-08-16T14:56:03.031Z");
        assertTimestamp(1471359363031L, "2016-08-16 14:56:03.031 +00:00");
        assertTimestamp(1471359363031L, "2016-08-16 14:56:03.031 +0000");
        assertTimestamp(1471359363031L, "2016-08-16 14:56:03.031 Z");
        assertTimestamp(1471359363031L, "2016-08-16 14:56:03.031 UTC");

        assertInvalid("2016-08-16T14:56:03.031 -07:00");
        assertInvalid("2016-08-16T14:56:03.031 -0700");
        assertInvalid("2016-08-16 14:56:03.031-07:00");
        assertInvalid("2016-08-16 14:56:03.031-0700");

        assertInvalid("2016-08-16T14:56:03.031 +00:00");
        assertInvalid("2016-08-16T14:56:03.031 +0000");
        assertInvalid("2016-08-16T14:56:03.031 Z");
        assertInvalid("2016-08-16T14:56:03.031 UTC");
        assertInvalid("2016-08-16 14:56:03.031+00:00");
        assertInvalid("2016-08-16 14:56:03.031+0000");
        assertInvalid("2016-08-16 14:56:03.031Z");
        assertInvalid("2016-08-16 14:56:03.031UTC");

        assertInvalid("2016-08-16 14:56:03.0315");
        assertInvalid("2016-08-16 14:56:03.03156");
        assertInvalid("2016-08-16 14:56:03.031567");
        assertInvalid("2016-08-16 14:56:03.03156799321");
    }

    @Test
    public void testUnix()
        throws Exception
    {
        assertTimestamp(1471359363000L, 1471359363L);
    }

    @Test
    public void testFormats()
        throws Exception
    {
        assertEquals("2016-08-16T14:56:03Z", TimestampParam.of(Timestamp.ofEpochMilli(1471359363000L)).toString());
        assertEquals("2016-08-16T14:56:03.031Z", TimestampParam.of(Timestamp.ofEpochMilli(1471359363031L)).toString());
    }

    private static void assertInvalid(String string)
    {
        try {
            TimestampParam.fromJson(JsonNodeFactory.instance.textNode(string)).getTimestamp();
            fail();
        }
        catch (Exception ex) {
        }
    }

    private static void assertTimestamp(long millis, String string)
        throws Exception
    {
        assertEquals(
                Timestamp.ofEpochMilli(millis),
                TimestampParam.fromJson(JsonNodeFactory.instance.textNode(string)).getTimestamp());
    }

    private static void assertTimestamp(long millis, long num)
        throws Exception
    {
        assertEquals(
                Timestamp.ofEpochMilli(millis),
                TimestampParam.fromJson(JsonNodeFactory.instance.numberNode(num)).getTimestamp());
    }
}
