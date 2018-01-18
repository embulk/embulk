package org.embulk.spi.time;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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

    /**
     * Converts org.joda.time.DateTimeZone to its corresponding java.time.ZoneID.
     */
    public static ZoneId convertJodaDateTimeZoneToZoneId(final org.joda.time.DateTimeZone jodaDateTimeZone) {
        return ZoneId.of(jodaDateTimeZone.getID(), ALIAS_ZONE_IDS_FOR_LEGACY);
    }

    /**
     * Converts java.time.ZoneOffset to its corresponding org.joda.time.DateTimeZone.
     */
    public static org.joda.time.DateTimeZone convertZoneOffsetToJodaDateTimeZone(final ZoneOffset zoneOffset) {
        return org.joda.time.DateTimeZone.forOffsetMillis(zoneOffset.getTotalSeconds() * 1000);
    }

    /**
     * Parses a time zone ID to java.time.ZoneOffset basically in the same rule with Ruby v2.3.1's Time.strptime.
     *
     * The only difference from Ruby v2.3.1's Time.strptime is that it does not consider local time zone.
     * If the given zone is neither numerical nor predefined textual time zones, it returns defaultZoneOffset then.
     *
     * The method is reimplemented based on zone_offset from Ruby v2.3.1's lib/time.rb.
     *
     * @see <a href="https://svn.ruby-lang.org/cgi-bin/viewvc.cgi/tags/v2_3_1/lib/time.rb?view=markup#l134">zone_offset</a>
     */
    public static ZoneOffset parseRubyTimeZoneOffset(final String zoneId, final ZoneOffset defaultZoneOffset) {
        if (zoneId == null) {
            return defaultZoneOffset;
        }

        if (zoneId.charAt(0) == '+' || zoneId.charAt(0) == '-') {
            if (RUBY_TIME_ZONE_OFFSET_PATTERN_HH_MM.matcher(zoneId).matches()
                    || RUBY_TIME_ZONE_OFFSET_PATTERN_HH.matcher(zoneId).matches()) {
                return ZoneOffset.of(zoneId);
            }
            return defaultZoneOffset;
        }

        final ZoneOffset zoneOffset = RUBY_TIME_ZONE_OFFSET_IDS.get(zoneId.toUpperCase(Locale.ENGLISH));
        if (zoneOffset != null) {
            return zoneOffset;
        }
        return defaultZoneOffset;
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
     * Its time offset transitions in each time zone may be different from the time zone parser of Embulk v0.8,
     * but the difference is from their base time zone (tz) database. The difference is ignorable as time zone
     * database is continuously updated anyway.
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

    static {
        final HashMap<String, String> aliasZoneIdsForLegacyParser = new HashMap<>();
        // Embulk's legacy TimestampParser has recognized "EST", "EDT", "CST", "CDT", "MST", "MDT", "PST", and "PDT"
        // with org.joda.time.format.DateTimeFormat.forPattern("z").parseMillis(). They have not been recognized
        // by org.joda.time.DateTimeZone.forID(), nor listed in org.joda.time.DateTimeZone.getAvailableIDs().
        //
        // These short time zone IDs have not been recognized in a "correct" way in Embulk's legacy TimestampParser,
        // though. For example, "PDT" should be -07:00 as it is daylight saving time. But, "PDT" and "PST" are both
        // recognized as -08:00 in Embulk's legacy TimestampParser.
        //
        // It has been because Embulk has used org.joda.time.format.DateTimeFormat.forPattern("z").parseMillis().
        // "PDT" is recognized as "America/Los_Angeles" in Joda-Time, which is a "not fixed" time zone.
        // https://github.com/JodaOrg/joda-time/blob/v2.9.2/src/main/java/org/joda/time/DateTimeUtils.java#L446
        // http://www.joda.org/joda-time/apidocs/org/joda/time/DateTimeUtils.html#getDefaultTimeZoneNames--
        //
        // "2017-02-20 America/Los_Angeles" is in -08:00 in Joda-Time. "2017-08-20 America/Los_Angeles" is in -7:00.
        // org.joda.time.format.DateTimeFormat.forPattern("z").parseMillis("America/Los_Angeles") is, however, always
        // -08:00 since it does not have the date.
        //
        // This set of aliases emulates org.joda.time.format.DateTimeFormat.forPattern("z").parseMillis() here
        // for compatibility. Embulk has used it to parse time zones for a very long time since Embulk v0.1.
        // https://github.com/embulk/embulk/commit/b97954a5c78397e1269bbb6979d6225dfceb4e05
        // https://github.com/embulk/embulk/issues/860
        aliasZoneIdsForLegacyParser.put("EST", "-05:00");
        aliasZoneIdsForLegacyParser.put("EDT", "-05:00");
        aliasZoneIdsForLegacyParser.put("CST", "-06:00");
        aliasZoneIdsForLegacyParser.put("CDT", "-06:00");
        aliasZoneIdsForLegacyParser.put("MST", "-07:00");
        aliasZoneIdsForLegacyParser.put("MDT", "-07:00");
        aliasZoneIdsForLegacyParser.put("PST", "-08:00");
        aliasZoneIdsForLegacyParser.put("PDT", "-08:00");

        // Short time zone IDs "EST", "HST", "MST", and "ROC" are recognized by org.joda.time.DateTimeZone.forID(),
        // while they are not recognized by java.time.ZoneId.of(). "EST" and "MST" are covered by the aliaes above
        // in higher priority. "HST" and "ROC" should be covered in addition.
        // http://joda-time.sourceforge.net/timezones.html
        aliasZoneIdsForLegacyParser.put("HST", "-10:00");
        aliasZoneIdsForLegacyParser.put("ROC", "Asia/Taipei");

        ALIAS_ZONE_IDS_FOR_LEGACY = Collections.unmodifiableMap(aliasZoneIdsForLegacyParser);

        final HashMap<String, ZoneOffset> rubyTimeZoneOffsetIds = new HashMap<>();
        rubyTimeZoneOffsetIds.put("UTC", ZoneOffset.UTC);
        // ISO 8601
        rubyTimeZoneOffsetIds.put("Z", ZoneOffset.UTC);
        // RFC 822
        rubyTimeZoneOffsetIds.put("UT", ZoneOffset.UTC);
        rubyTimeZoneOffsetIds.put("GMT", ZoneOffset.UTC);
        rubyTimeZoneOffsetIds.put("EST", ZoneOffset.ofHours(-5));
        rubyTimeZoneOffsetIds.put("EDT", ZoneOffset.ofHours(-4));
        rubyTimeZoneOffsetIds.put("CST", ZoneOffset.ofHours(-6));
        rubyTimeZoneOffsetIds.put("CDT", ZoneOffset.ofHours(-5));
        rubyTimeZoneOffsetIds.put("MST", ZoneOffset.ofHours(-7));
        rubyTimeZoneOffsetIds.put("MDT", ZoneOffset.ofHours(-6));
        rubyTimeZoneOffsetIds.put("PST", ZoneOffset.ofHours(-8));
        rubyTimeZoneOffsetIds.put("PDT", ZoneOffset.ofHours(-7));
        // Following definition of military zones is original one.
        // See RFC 1123 and RFC 2822 for the error in RFC 822.
        rubyTimeZoneOffsetIds.put("A", ZoneOffset.ofHours(+1));
        rubyTimeZoneOffsetIds.put("B", ZoneOffset.ofHours(+2));
        rubyTimeZoneOffsetIds.put("C", ZoneOffset.ofHours(+3));
        rubyTimeZoneOffsetIds.put("D", ZoneOffset.ofHours(+4));
        rubyTimeZoneOffsetIds.put("E", ZoneOffset.ofHours(+5));
        rubyTimeZoneOffsetIds.put("F", ZoneOffset.ofHours(+6));
        rubyTimeZoneOffsetIds.put("G", ZoneOffset.ofHours(+7));
        rubyTimeZoneOffsetIds.put("H", ZoneOffset.ofHours(+8));
        rubyTimeZoneOffsetIds.put("I", ZoneOffset.ofHours(+9));
        rubyTimeZoneOffsetIds.put("K", ZoneOffset.ofHours(+10));
        rubyTimeZoneOffsetIds.put("L", ZoneOffset.ofHours(+11));
        rubyTimeZoneOffsetIds.put("M", ZoneOffset.ofHours(+12));
        rubyTimeZoneOffsetIds.put("N", ZoneOffset.ofHours(-1));
        rubyTimeZoneOffsetIds.put("O", ZoneOffset.ofHours(-2));
        rubyTimeZoneOffsetIds.put("P", ZoneOffset.ofHours(-3));
        rubyTimeZoneOffsetIds.put("Q", ZoneOffset.ofHours(-4));
        rubyTimeZoneOffsetIds.put("R", ZoneOffset.ofHours(-5));
        rubyTimeZoneOffsetIds.put("S", ZoneOffset.ofHours(-6));
        rubyTimeZoneOffsetIds.put("T", ZoneOffset.ofHours(-7));
        rubyTimeZoneOffsetIds.put("U", ZoneOffset.ofHours(-8));
        rubyTimeZoneOffsetIds.put("V", ZoneOffset.ofHours(-9));
        rubyTimeZoneOffsetIds.put("W", ZoneOffset.ofHours(-10));
        rubyTimeZoneOffsetIds.put("X", ZoneOffset.ofHours(-11));
        rubyTimeZoneOffsetIds.put("Y", ZoneOffset.ofHours(-12));
        RUBY_TIME_ZONE_OFFSET_IDS = Collections.unmodifiableMap(rubyTimeZoneOffsetIds);
    }

    static final Map<String, String> ALIAS_ZONE_IDS_FOR_LEGACY;

    private static final Set<String> JODA_TIME_ZONES =
            Collections.unmodifiableSet(org.joda.time.DateTimeZone.getAvailableIDs());

    private static final Pattern RUBY_TIME_ZONE_OFFSET_PATTERN_HH_MM = Pattern.compile("^[+-]\\d\\d:?\\d\\d$");
    private static final Pattern RUBY_TIME_ZONE_OFFSET_PATTERN_HH = Pattern.compile("^[+-]\\d\\d$");

    private static final Map<String, ZoneOffset> RUBY_TIME_ZONE_OFFSET_IDS;
}
