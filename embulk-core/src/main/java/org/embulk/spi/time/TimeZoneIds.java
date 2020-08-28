package org.embulk.spi.time;

import java.util.Collections;
import java.util.Set;

/**
 * TimeZoneIds is a utility class for operating with time zones.
 *
 * This class is public only to be called from DynamicColumnSetterFactory and DynamicPageBuilder.
 * It is not guaranteed to use this class from plugins. This class may be moved, renamed, or removed.
 *
 * A part of this class is reimplementation of Ruby v2.3.1's lib/time.rb. See its COPYING for license.
 *
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup">lib/time.rb</a>
 * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/COPYING?view=markup">COPYING</a>
 */
public class TimeZoneIds {
    private TimeZoneIds() {
        // No instantiation.
    }

    public static org.joda.time.DateTimeZone parseJodaDateTimeZone(final String timeZoneName) {
        org.joda.time.DateTimeZone jodaDateTimeZoneTemporary = null;
        try {
            // Use TimeZone#forID, not TimeZone#getTimeZone.
            // Because getTimeZone returns GMT even if given timezone id is not found.
            jodaDateTimeZoneTemporary = org.joda.time.DateTimeZone.forID(timeZoneName);
        } catch (IllegalArgumentException ex) {
            jodaDateTimeZoneTemporary = null;
        }
        final org.joda.time.DateTimeZone jodaDateTimeZone = jodaDateTimeZoneTemporary;

        // Embulk has accepted to parse Joda-Time's time zone IDs in Timestamps since v0.2.0
        // although the formats are based on Ruby's strptime. Joda-Time's time zone IDs are
        // continuously to be accepted with higher priority than Ruby's time zone IDs.
        if (jodaDateTimeZone != null && (timeZoneName.startsWith("+") || timeZoneName.startsWith("-"))) {
            return jodaDateTimeZone;

        } else if (timeZoneName.equals("Z")) {
            return org.joda.time.DateTimeZone.UTC;

        } else {
            try {
                // DateTimeFormat.forPattern("z").parseMillis(s) is incorrect, but kept for compatibility as of now.
                //
                // The offset of PDT (Pacific Daylight Time) should be -07:00.
                // DateTimeFormat.forPattern("z").parseMillis("PDT") however returns 8 hours (-08:00).
                // DateTimeFormat.forPattern("z").parseMillis("PDT") == 28800000
                // https://github.com/JodaOrg/joda-time/blob/v2.9.2/src/main/java/org/joda/time/DateTimeUtils.java#L446
                //
                // Embulk has used it to parse time zones for a very long time since it was v0.1.
                // https://github.com/embulk/embulk/commit/b97954a5c78397e1269bbb6979d6225dfceb4e05
                //
                // It is kept as -08:00 for compatibility as of now.
                //
                // TODO: Make time zone parsing consistent.
                // @see <a href="https://github.com/embulk/embulk/issues/860">https://github.com/embulk/embulk/issues/860</a>
                int rawOffset = (int) org.joda.time.format.DateTimeFormat.forPattern("z").parseMillis(timeZoneName);
                if (rawOffset == 0) {
                    return org.joda.time.DateTimeZone.UTC;
                }
                int offset = rawOffset / -1000;
                int h = offset / 3600;
                int m = offset % 3600;
                return org.joda.time.DateTimeZone.forOffsetHoursMinutes(h, m);
            } catch (IllegalArgumentException ex) {
                // parseMillis failed
            }

            if (jodaDateTimeZone != null && JODA_TIME_ZONES.contains(timeZoneName)) {
                return jodaDateTimeZone;
            }

            // Parsing Ruby-style time zones in lower priority than Joda-Time because
            // TimestampParser has parsed time zones with Joda-Time for a long time
            // since ancient. The behavior is kept for compatibility.
            //
            // The following time zone IDs are duplicated in Ruby and Joda-Time 2.9.2
            // while Ruby does not care summer time and Joda-Time cares summer time.
            // "CET", "EET", "Egypt", "Iran", "MET", "WET"
            //
            // Some zone IDs (ex. "PDT") are parsed by DateTimeFormat#parseMillis as shown above.
            final int rubyStyleTimeOffsetInSecond = RubyTimeZoneTab.dateZoneToDiff(timeZoneName);
            if (rubyStyleTimeOffsetInSecond != Integer.MIN_VALUE) {
                return org.joda.time.DateTimeZone.forOffsetMillis(rubyStyleTimeOffsetInSecond * 1000);
            }

            return null;
        }
    }

    private static final Set<String> JODA_TIME_ZONES =
            Collections.unmodifiableSet(org.joda.time.DateTimeZone.getAvailableIDs());
}
