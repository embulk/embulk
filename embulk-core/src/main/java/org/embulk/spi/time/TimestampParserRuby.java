package org.embulk.spi.time;

import com.google.common.base.Strings;
import java.time.Instant;
import java.time.ZoneOffset;

public class TimestampParserRuby extends TimestampParser {
    private TimestampParserRuby(final RubyTimeParser parser,
                                final ZoneOffset defaultZoneOffset,
                                final String formatString) {
        this.parser = parser;
        this.defaultZoneOffset = defaultZoneOffset;
        this.formatString = formatString;
    }

    static TimestampParserRuby of(final String formatString, final ZoneOffset defaultZoneOffset) {
        return new TimestampParserRuby(new RubyTimeParser(RubyTimeFormat.compile(formatString)),
                                       defaultZoneOffset,
                                       formatString);
    }

    // Using Joda-Time is deprecated, but the method return org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    @Override
    public org.joda.time.DateTimeZone getDefaultTimeZone() {
        return TimeZoneIds.convertZoneOffsetToJodaDateTimeZone(this.defaultZoneOffset);
    }

    @Override
    Instant parseInternal(final String text) throws TimestampParseException {
        if (Strings.isNullOrEmpty(text)) {
            throw new TimestampParseException("text is null or empty string.");
        }

        final RubyTimeParsed parsed = this.parser.parse(text);
        if (parsed == null) {
            throw new TimestampParseException("Cannot parse '" + text + "' by '" + this.formatString + "'");
        }
        return parsed.toInstant(this.defaultZoneOffset);
    }

    private final RubyTimeParser parser;
    private final ZoneOffset defaultZoneOffset;
    private final String formatString;
}
