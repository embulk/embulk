package org.embulk.spi.time;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.TimeZone;
import org.embulk.deps.timestamp.RubyDateTimeFormatter;

public class TimestampFormatterRuby extends TimestampFormatter {
    private TimestampFormatterRuby(final RubyDateTimeFormatter formatter,
                                   final ZoneId zoneId,
                                   final String formatString) {
        this.formatter = formatter;
        this.zoneId = zoneId;
        this.formatString = formatString;
    }

    static TimestampFormatterRuby of(final String formatString, final ZoneOffset zoneOffset) {
        return new TimestampFormatterRuby(RubyDateTimeFormatter.create(formatString),
                                          zoneOffset,
                                          formatString);
    }

    static TimestampFormatterRuby ofLegacy(final String formatString, final ZoneId zoneId) {
        return new TimestampFormatterRuby(RubyDateTimeFormatter.create(formatString),
                                          zoneId,
                                          formatString);
    }

    // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    @Override
    public org.joda.time.DateTimeZone getTimeZone() {
        if (this.zoneId instanceof ZoneOffset) {
            return TimeZoneIds.convertZoneOffsetToJodaDateTimeZone((ZoneOffset) this.zoneId);
        } else {
            return org.joda.time.DateTimeZone.forTimeZone(TimeZone.getTimeZone(this.zoneId));
        }
    }

    public String format(final Timestamp value) {
        if (this.zoneId instanceof ZoneOffset) {
            return this.formatter.format(OffsetDateTime.ofInstant(value.getInstant(), this.zoneId));
        } else {
            return this.formatter.format(ZonedDateTime.ofInstant(value.getInstant(), this.zoneId));
        }
    }

    private final RubyDateTimeFormatter formatter;
    private final ZoneId zoneId;  // Nullable
    private final String formatString;
}
