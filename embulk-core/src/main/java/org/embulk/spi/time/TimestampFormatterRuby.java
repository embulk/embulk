package org.embulk.spi.time;

import java.time.ZoneOffset;
import java.util.Locale;

public class TimestampFormatterRuby extends TimestampFormatter {
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/830
    private TimestampFormatterRuby(final org.jruby.util.RubyDateFormat formatter,
                                   final ZoneOffset zoneOffset,
                                   final org.joda.time.DateTimeZone jodaDateTimeZone,
                                   final String formatString) {
        this.formatter = formatter;
        this.zoneOffset = zoneOffset;
        this.jodaDateTimeZone = jodaDateTimeZone;
        this.formatString = formatString;
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/830
    static TimestampFormatterRuby of(final String formatString, final ZoneOffset zoneOffset) {
        return new TimestampFormatterRuby(new org.jruby.util.RubyDateFormat(formatString, Locale.ENGLISH, true),
                                          zoneOffset,
                                          TimeZoneIds.convertZoneOffsetToJodaDateTimeZone(zoneOffset),
                                          formatString);
    }

    // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/830
    static TimestampFormatterRuby ofLegacy(final String formatString,
                                           final org.joda.time.DateTimeZone jodaDateTimeZone) {
        return new TimestampFormatterRuby(new org.jruby.util.RubyDateFormat(formatString, Locale.ENGLISH, true),
                                          null,
                                          jodaDateTimeZone,
                                          formatString);
    }

    // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    @Override
    public org.joda.time.DateTimeZone getTimeZone() {
        return this.jodaDateTimeZone;
    }

    public String format(final Timestamp value) {
        // TODO: Optimize by using reused StringBuilder.
        this.formatter.setDateTime(new org.joda.time.DateTime(value.getEpochSecond() * 1000, this.jodaDateTimeZone));
        this.formatter.setNSec(value.getNano());
        return this.formatter.format(null);
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/830
    private final org.jruby.util.RubyDateFormat formatter;
    private final ZoneOffset zoneOffset;  // Nullable
    private final org.joda.time.DateTimeZone jodaDateTimeZone;  // Not null
    private final String formatString;
}
