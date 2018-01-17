package org.embulk.spi.time;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class TimestampFormatterJava extends TimestampFormatter {
    private TimestampFormatterJava(final DateTimeFormatter formatter,
                                   final ZoneOffset zoneOffset,
                                   final String pattern) {
        this.formatter = formatter;
        this.zoneOffset = zoneOffset;
        this.pattern = pattern;
    }

    static TimestampFormatterJava of(final String pattern, final ZoneOffset zoneOffset) {
        return new TimestampFormatterJava(new DateTimeFormatterBuilder()
                                                  .appendPattern(pattern)
                                                  .toFormatter(Locale.ENGLISH),
                                          zoneOffset,
                                          pattern);
    }

    static TimestampFormatterJava of(final DateTimeFormatter formatter, final ZoneOffset zoneOffset) {
        return new TimestampFormatterJava(formatter, zoneOffset, "");
    }

    // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    @Override
    public org.joda.time.DateTimeZone getTimeZone() {
        return TimeZoneIds.convertZoneOffsetToJodaDateTimeZone(this.zoneOffset);
    }

    @Override
    public String format(final Timestamp value) {
        return this.formatter.format(OffsetDateTime.ofInstant(value.getInstant(), this.zoneOffset));
    }

    private final DateTimeFormatter formatter;
    private final ZoneOffset zoneOffset;
    private final String pattern;
}
