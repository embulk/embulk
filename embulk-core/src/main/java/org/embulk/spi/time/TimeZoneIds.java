package org.embulk.spi.time;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * TimeZoneIds is a utility class for operating with time zones.
 */
class TimeZoneIds
{
    private TimeZoneIds() {
        // No instantiation.
    }

    /**
     * Parses time zone ID to java.time.ZoneId as compatible with the time zone parser of Embulk v0.8 as possible.
     *
     * It recognizes time zone IDs in the following priority.
     *
     * <ol>
     * <li>"Z" is always recognized as UTC in the first priority.
     * <li>If the ID is "EST", "EDT", "CST", "CDT", "MST", "MDT", "PST", or "PDT", parsed by ZoneId.of with alias.
     * <li>If the ID is "HST", "ROC", or recognized by ZoneId.of, it is parsed by ZoneId.of with alias.
     * <li>Otherwise, the zone ID is recognized by Ruby-compatible zone tab.
     * <li>If none of the above does not recognize the zone ID, it returns null.
     * </ol>
     *
     * It differs from the time zone parser as of Embulk v0.8 in terms of the following short time zone names:
     * "EST", "EDT", "CST", "CDT", "MST", "MDT", "PST", and "PDT". They were recognized as fixed standard time
     * (non-summer time) in v0.8. This parser's results are aware of summer time.
     *
     * Another difference is time offset transitions in each time zone, but the difference is from their base
     * time zone database. The difference is ignorable as time zone database is continuously updated anyway.
     */
    public static ZoneId parseZoneIdWithJodaAndRubyZoneTab(final String zoneId) {
        if (zoneId.equals("Z")) {
            return ZoneOffset.UTC;
        }

        try {
            return ZoneId.of(zoneId, ALIAS_ZONE_IDS_FOR_LEGACY);  // Is is never null unless Exception is thrown.
        } catch (DateTimeException ex) {
            final int rubyStyleTimeOffsetInSecond = RubyTimeZoneTab.dateZoneToDiff(zoneId);
            if (rubyStyleTimeOffsetInSecond != Integer.MIN_VALUE) {
                return ZoneOffset.ofTotalSeconds(rubyStyleTimeOffsetInSecond);
            }
            return null;
        }
    }

    public static org.joda.time.DateTimeZone parseJodaDateTimeZone(final String timeZoneName) {
        org.joda.time.DateTimeZone jodaDateTimeZoneTemporary = null;
        try {
            // Use TimeZone#forID, not TimeZone#getTimeZone.
            // Because getTimeZone returns GMT even if given timezone id is not found.
            jodaDateTimeZoneTemporary = org.joda.time.DateTimeZone.forID(timeZoneName);
        }
        catch (IllegalArgumentException ex) {
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

    static {
        final HashMap<String, String> aliasZoneIdsForLegacyParser = new HashMap<>();
        // org.joda.time.format.DateTimeFormat.forPattern("z").parseMillis() recognizes short time zone names
        // "EST", "EDT", "CST", "CDT", "MST", "MDT", "PST", and "PDT" although they are not recognized by
        // org.joda.time.DateTimeZone.forID(), and not listed in org.joda.time.DateTimeZone.getAvailableIDs().
        //
        // Default ZoneId.of() does not recognize these short time zone names although the time zone parser of
        // Embulk v0.8 recognized them with org.joda.time.format.DateTimeFormat.forPattern("z").parseMillis()
        // in priority.
        //
        // See: http://www.joda.org/joda-time/apidocs/org/joda/time/DateTimeUtils.html#getDefaultTimeZoneNames--
        aliasZoneIdsForLegacyParser.put("EST", "America/New_York");
        aliasZoneIdsForLegacyParser.put("EDT", "America/New_York");
        aliasZoneIdsForLegacyParser.put("CST", "America/Chicago");
        aliasZoneIdsForLegacyParser.put("CDT", "America/Chicago");
        aliasZoneIdsForLegacyParser.put("MST", "America/Denver");
        aliasZoneIdsForLegacyParser.put("MDT", "America/Denver");
        aliasZoneIdsForLegacyParser.put("PST", "America/Los_Angeles");
        aliasZoneIdsForLegacyParser.put("PDT", "America/Los_Angeles");

        // Short time zone names "HST" and "ROC" are listed in org.joda.time.DateTimeZone.getAvailableIDs(), and
        // recognized by org.joda.time.DateTimeZone.forID() while they are not recognized by ZoneId.of().
        //
        // See: http://joda-time.sourceforge.net/timezones.html
        aliasZoneIdsForLegacyParser.put("HST", "-10:00");
        aliasZoneIdsForLegacyParser.put("ROC", "Asia/Taipei");

        // Short time zone names "EST" and "MST" are listed in org.joda.time.DateTimeZone.getAvailableIDs(), and
        // recognized by org.joda.time.DateTimeZone.forID(). But, the time zone parser of Embulk v0.8 recognized
        // them with org.joda.time.format.DateTimeFormat.forPattern("z").parseMillis() in higher priority.

        // "GMT+0" and "GMT-0" are recognized by ZoneId.of() while they are not in ZoneId.getAvailableZoneIds().
        // They are listed in org.joda.time.DateTimeZone.getAvailableIDs().

        ALIAS_ZONE_IDS_FOR_LEGACY = Collections.unmodifiableMap(aliasZoneIdsForLegacyParser);
    }

    static final Map<String, String> ALIAS_ZONE_IDS_FOR_LEGACY;

    private static final Set<String> JODA_TIME_ZONES =
        Collections.unmodifiableSet(org.joda.time.DateTimeZone.getAvailableIDs());
}
