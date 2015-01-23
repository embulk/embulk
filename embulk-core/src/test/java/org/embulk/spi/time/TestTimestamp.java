package org.embulk.spi.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class TestTimestamp
{
    @Test
    public void testEqualsToTimestamp()
    {
        assertEqualsMethods(Timestamp.ofEpochSecond(0), Timestamp.ofEpochSecond(0));
        assertEqualsMethods(Timestamp.ofEpochSecond(10), Timestamp.ofEpochSecond(10));
        assertEqualsMethods(Timestamp.ofEpochSecond(10, 2), Timestamp.ofEpochSecond(10, 2));
    }

    private void assertEqualsMethods(Timestamp t1, Timestamp t2)
    {
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertEquals(0, t1.compareTo(t2));
    }

    @Test
    public void testNotEqualsToTimestamp()
    {
        assertFalse(Timestamp.ofEpochSecond(0).equals(Timestamp.ofEpochSecond(1)));
        assertFalse(Timestamp.ofEpochSecond(10).equals(Timestamp.ofEpochSecond(10, 2)));
        assertFalse(Timestamp.ofEpochSecond(10, 2).equals(Timestamp.ofEpochSecond(20, 2)));
    }

    @Test
    public void testEqualsToNull()
    {
        assertFalse(Timestamp.ofEpochSecond(0).equals(null));
        assertFalse(Timestamp.ofEpochSecond(1, 2).equals(null));
    }

    @Test
    public void testEqualsOtherClass()
    {
        assertFalse(Timestamp.ofEpochSecond(0).equals(new Object()));
        assertFalse(Timestamp.ofEpochSecond(1, 2).equals("other"));
    }

    @Test
    public void testAdjustMillisToNanos()
    {
        Timestamp t = Timestamp.ofEpochMilli(3);  // 3 msec = 3_000 usec == 3_000_000 nsec
        assertEquals(0L, t.getEpochSecond());
        assertEquals(3_000_000, t.getNano());
    }

    @Test
    public void testAdjustMillisToSeconds()
    {
        Timestamp t = Timestamp.ofEpochMilli(3_000);  // 3_000 msec = 3 sec
        assertEquals(3L, t.getEpochSecond());
        assertEquals(0, t.getNano());
    }

    @Test
    public void testAdjustNano()
    {
        Timestamp t = Timestamp.ofEpochSecond(0, 1_000_000_000);  // 1_000_000_000 nsec = 1_000_000 usec = 1_000 msec = 1 sec
        assertEquals(1L, t.getEpochSecond());
        assertEquals(0, t.getNano());
    }

    @Test
    public void testCompareTo()
    {
        assertEquals(-1, Timestamp.ofEpochSecond(3).compareTo(Timestamp.ofEpochSecond(4)));
        assertEquals(-1, Timestamp.ofEpochSecond(3).compareTo(Timestamp.ofEpochSecond(3, 4)));
        assertEquals( 1, Timestamp.ofEpochSecond(4).compareTo(Timestamp.ofEpochSecond(3)));
        assertEquals( 1, Timestamp.ofEpochSecond(3, 4).compareTo(Timestamp.ofEpochSecond(3)));
    }

    @Test
    public void testToString()
    {
        assertEquals("1970-01-01 00:00:00 UTC", Timestamp.ofEpochSecond(0).toString());
        assertEquals("2015-01-19 07:36:10 UTC", Timestamp.ofEpochSecond(1421652970).toString());
        assertEquals("2015-01-19 07:36:10.100 UTC",       Timestamp.ofEpochSecond(1421652970, 100*1000*1000).toString());
        assertEquals("2015-01-19 07:36:10.120 UTC",       Timestamp.ofEpochSecond(1421652970, 120*1000*1000).toString());
        assertEquals("2015-01-19 07:36:10.123 UTC",       Timestamp.ofEpochSecond(1421652970, 123*1000*1000).toString());
        assertEquals("2015-01-19 07:36:10.123400 UTC",    Timestamp.ofEpochSecond(1421652970, 123400*1000).toString());
        assertEquals("2015-01-19 07:36:10.123450 UTC",    Timestamp.ofEpochSecond(1421652970, 123450*1000).toString());
        assertEquals("2015-01-19 07:36:10.123456 UTC",    Timestamp.ofEpochSecond(1421652970, 123456*1000).toString());
        assertEquals("2015-01-19 07:36:10.123456700 UTC", Timestamp.ofEpochSecond(1421652970, 123456700).toString());
        assertEquals("2015-01-19 07:36:10.123456780 UTC", Timestamp.ofEpochSecond(1421652970, 123456780).toString());
        assertEquals("2015-01-19 07:36:10.123456789 UTC", Timestamp.ofEpochSecond(1421652970, 123456789).toString());
    }

    @Test
    public void testFromString()
    {
        checkToStringFromString(Timestamp.ofEpochSecond(0));
        checkToStringFromString(Timestamp.ofEpochSecond(1421652970));
        checkToStringFromString(Timestamp.ofEpochSecond(1421652970, 100*1000*1000));
        checkToStringFromString(Timestamp.ofEpochSecond(1421652970, 120*1000*1000));
        checkToStringFromString(Timestamp.ofEpochSecond(1421652970, 123*1000*1000));
        checkToStringFromString(Timestamp.ofEpochSecond(1421652970, 123400*1000));
        checkToStringFromString(Timestamp.ofEpochSecond(1421652970, 123450*1000));
        checkToStringFromString(Timestamp.ofEpochSecond(1421652970, 123456*1000));
        checkToStringFromString(Timestamp.ofEpochSecond(1421652970, 123456700));
        checkToStringFromString(Timestamp.ofEpochSecond(1421652970, 123456780));
        checkToStringFromString(Timestamp.ofEpochSecond(1421652970, 123456789));
    }

    private void checkToStringFromString(Timestamp timestamp)
    {
        assertEquals(timestamp, Timestamp.fromString(timestamp.toString()));
    }
}
