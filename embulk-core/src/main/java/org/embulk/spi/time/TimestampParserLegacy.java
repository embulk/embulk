package org.embulk.spi.time;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.embulk.config.ConfigException;

public class TimestampParserLegacy extends TimestampParser {
    private TimestampParserLegacy(final String formatString,
                                  final ZoneId defaultZoneId,
                                  final org.joda.time.DateTimeZone defaultJodaDateTimeZone,
                                  final int defaultYear,
                                  final int defaultMonthOfYear,
                                  final int defaultDayOfMonth) {
        this.formatString = formatString;
        this.parser = new RubyTimeParser(RubyTimeFormat.compile(formatString));
        this.defaultJodaDateTimeZone = defaultJodaDateTimeZone;
        this.defaultZoneId = defaultZoneId;
        this.defaultYear = defaultYear;
        this.defaultMonthOfYear = defaultMonthOfYear;
        this.defaultDayOfMonth = defaultDayOfMonth;
    }

    static TimestampParserLegacy of(final String formatString,
                                    final ZoneId defaultZoneId,
                                    final org.joda.time.DateTimeZone defaultJodaDateTimeZone,
                                    final int defaultYear,
                                    final int defaultMonthOfYear,
                                    final int defaultDayOfMonth) {
        return new TimestampParserLegacy(formatString,
                                         defaultZoneId,
                                         defaultJodaDateTimeZone,
                                         defaultYear,
                                         defaultMonthOfYear,
                                         defaultDayOfMonth);
    }

    static TimestampParserLegacy of(final String formatString,
                                    final ZoneId defaultZoneId,
                                    final org.joda.time.DateTimeZone defaultJodaDateTimeZone,
                                    final String defaultDateString) {
        final LocalDate defaultDate = parseDateForDefault(defaultDateString);
        return of(formatString,
                  defaultZoneId,
                  defaultJodaDateTimeZone,
                  defaultDate.getYear(),
                  defaultDate.getMonthValue(),
                  defaultDate.getDayOfMonth());
    }

    @VisibleForTesting
    static TimestampParser createTimestampParserForTesting(final TimestampParser.Task task) {
        return of(task.getDefaultTimestampFormat(),
                  TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab(task.getDefaultTimeZoneId()),
                  TimeZoneIds.parseJodaDateTimeZone(task.getDefaultTimeZoneId()),
                  task.getDefaultDate());
    }

    // Using Joda-Time is deprecated, but the method return org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    @Override
    public org.joda.time.DateTimeZone getDefaultTimeZone() {
        return defaultJodaDateTimeZone;
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
        return parsed.toLegacy().toInstant(this.defaultYear,
                                           this.defaultMonthOfYear,
                                           this.defaultDayOfMonth,
                                           this.defaultZoneId);
    }

    private static LocalDate parseDateForDefault(final String defaultDate) {
        try {
            return LocalDate.parse(defaultDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException ex) {
            throw new ConfigException("Invalid date format. Expected yyyy-MM-dd: " + defaultDate, ex);
        }
    }

    private final String formatString;
    private final RubyTimeParser parser;

    private final org.joda.time.DateTimeZone defaultJodaDateTimeZone;
    private final ZoneId defaultZoneId;

    private final int defaultYear;
    private final int defaultMonthOfYear;
    private final int defaultDayOfMonth;
}
