package org.embulk.spi.time;

import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.Assert;
import org.junit.Test;

public class TestTimeZoneIds {
    @Test
    public void testParseZoneIdWithJodaAndRubyZoneTab() {
        // TODO: Test more practical equality. (Such as "GMT" v.s. "UTC")
        Assert.assertEquals(ZoneOffset.UTC, TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab("Z"));
        Assert.assertEquals(ZoneId.of("Asia/Tokyo"), TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab("Asia/Tokyo"));
        Assert.assertEquals(ZoneId.of("-05:00"), TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab("EST"));
        Assert.assertEquals(ZoneId.of("-10:00"), TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab("HST"));
        Assert.assertEquals(ZoneId.of("Asia/Taipei"), TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab("ROC"));
    }
}
