package org.embulk.spi.time;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

/**
 * TimeParsed is a container of date/time information parsed from a string.
 */
abstract class TimeParsed {  // to extend java.time.temporal.TemporalAccessor in Java 8
    public static abstract class Builder {
        public abstract TimeParsed build();
    }

    public static RubyTimeParsed.Builder rubyBuilder(final String originalString)
    {
        return new RubyTimeParsed.Builder(originalString);
    }

    abstract Instant toInstantLegacy(final int defaultYear,
                                     final int defaultMonthOfYear,
                                     final int defaultDayOfMonth,
                                     final ZoneId defaultZoneId);

    abstract Timestamp toTimestampLegacy(final int defaultYear,
                                         final int defaultMonthOfYear,
                                         final int defaultDayOfMonth,
                                         final ZoneId defaultZoneId);

    abstract public Map<String, Object> asMapLikeRubyHash();
}
