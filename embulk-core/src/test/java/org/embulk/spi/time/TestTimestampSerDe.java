package org.embulk.spi.time;

import org.junit.Assert;
import org.junit.Test;

public class TestTimestampSerDe
{
    @Test
    public void testCreateTimestampFromString()
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

    private void checkToStringFromString(final Timestamp timestamp)
    {
        Assert.assertEquals(timestamp, TimestampSerDe.createTimestampFromStringForTesting(timestamp.toString()));
    }
}
