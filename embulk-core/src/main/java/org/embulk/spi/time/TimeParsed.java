package org.embulk.spi.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * TimeParsed is a container of date/time information parsed from a string.
 */
abstract class TimeParsed {  // to extend java.time.temporal.TemporalAccessor in Java 8
    static RubyTimeParsed.Builder rubyBuilder(final String originalString) {
        return new RubyTimeParsed.Builder(originalString);
    }

    abstract Instant toInstant(final ZoneOffset defaultZoneOffset);

    abstract Instant toInstant(final int defaultYear,
                               final int defaultMonthOfYear,
                               final int defaultDayOfMonth,
                               final ZoneId defaultZoneId);
}
