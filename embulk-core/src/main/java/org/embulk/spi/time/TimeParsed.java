package org.embulk.spi.time;

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

    // TODO: Have java.time.Instant toInstant() in Java 8.

    abstract public Timestamp toTimestamp(final int defaultYear,
                                          final int defaultMonthOfYear,
                                          final int defaultDayOfMonth,
                                          final org.joda.time.DateTimeZone defaultTimeZone);

    abstract public Map<String, Object> asMapLikeRubyHash();
}
