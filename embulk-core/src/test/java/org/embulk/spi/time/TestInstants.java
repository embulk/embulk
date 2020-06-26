package org.embulk.spi.time;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import org.junit.Test;

public class TestInstants {
    @Test
    public void testCreateTimestampFromString() {
        assertToStringFromString("1970-01-01 00:00:00 UTC", Instant.ofEpochSecond(0));
        assertToStringFromString("2015-01-19 07:36:10 UTC", Instant.ofEpochSecond(1421652970));
        assertToStringFromString("2015-01-19 07:36:10.100 UTC", Instant.ofEpochSecond(1421652970, 100 * 1000 * 1000));
        assertToStringFromString("2015-01-19 07:36:10.120 UTC", Instant.ofEpochSecond(1421652970, 120 * 1000 * 1000));
        assertToStringFromString("2015-01-19 07:36:10.123 UTC", Instant.ofEpochSecond(1421652970, 123 * 1000 * 1000));
        assertToStringFromString("2015-01-19 07:36:10.123400 UTC", Instant.ofEpochSecond(1421652970, 123400 * 1000));
        assertToStringFromString("2015-01-19 07:36:10.123450 UTC", Instant.ofEpochSecond(1421652970, 123450 * 1000));
        assertToStringFromString("2015-01-19 07:36:10.123456 UTC", Instant.ofEpochSecond(1421652970, 123456 * 1000));
        assertToStringFromString("2015-01-19 07:36:10.123456700 UTC", Instant.ofEpochSecond(1421652970, 123456700));
        assertToStringFromString("2015-01-19 07:36:10.123456780 UTC", Instant.ofEpochSecond(1421652970, 123456780));
        assertToStringFromString("2015-01-19 07:36:10.123456789 UTC", Instant.ofEpochSecond(1421652970, 123456789));
    }

    private void assertToStringFromString(final String expectedString, final Instant instant) {
        final String actualString = Instants.toString(instant);
        assertEquals(expectedString, actualString);
        assertEquals(instant, Instants.parseInstant(actualString));
    }
}
