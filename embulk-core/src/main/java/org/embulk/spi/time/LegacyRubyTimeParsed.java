package org.embulk.spi.time;

import java.time.Instant;
import java.time.ZoneId;

/**
 * LegacyRubyTimeParsed is a variation of RubyTimeParsed, which is converted to Instant in legacy Embulk's way.
 */
class LegacyRubyTimeParsed extends RubyTimeParsed {
    LegacyRubyTimeParsed(
            final String originalString,

            final int dayOfMonth,
            final int weekBasedYear,
            final int hour,
            final int dayOfYear,
            final int nanoOfSecond,
            final int minuteOfHour,
            final int monthOfYear,
            final Instant instantSeconds,
            final int secondOfMinute,
            final int weekOfYearStartingWithSunday,
            final int weekOfYearStartingWithMonday,
            final int dayOfWeekStartingWithMonday1,
            final int weekOfWeekBasedYear,
            final int dayOfWeekStartingWithSunday0,
            final int year,

            final String timeZoneName,

            final String leftover) {
        super(originalString,

              dayOfMonth,
              weekBasedYear,
              hour,
              dayOfYear,
              nanoOfSecond,
              minuteOfHour,
              monthOfYear,
              instantSeconds,
              secondOfMinute,
              weekOfYearStartingWithSunday,
              weekOfYearStartingWithMonday,
              dayOfWeekStartingWithMonday1,
              weekOfWeekBasedYear,
              dayOfWeekStartingWithSunday0,
              year,

              timeZoneName,

              leftover);
    }

    @Override
    Instant toInstant(final int defaultYear,
                      final int defaultMonthOfYear,
                      final int defaultDayOfMonth,
                      final ZoneId defaultZoneId) {
        return this.toInstantLegacy(defaultYear,
                                    defaultMonthOfYear,
                                    defaultDayOfMonth,
                                    defaultZoneId);
    }
}
