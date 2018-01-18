package org.embulk.spi.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Locale;

/**
 * TimestampParserJava parses a String to org.embulk.spi.time.Timestamp with java.time.format.DateTimeFormatter.
 */
class TimestampParserJava extends TimestampParser {
    private TimestampParserJava(final DateTimeFormatter formatter,
                                final ZoneOffset defaultZoneOffset,
                                final String pattern) {
        this.formatter = formatter;
        this.defaultZoneOffset = defaultZoneOffset;
        this.pattern = pattern;
    }

    static TimestampParserJava of(final String pattern, final ZoneOffset defaultZoneOffset) {
        // Default parsers from patterns accept strings case-insensitively.
        return new TimestampParserJava(new DateTimeFormatterBuilder()
                                               .parseCaseInsensitive()
                                               .appendPattern(pattern)
                                               .toFormatter(Locale.ENGLISH)
                                               .withResolverStyle(ResolverStyle.STRICT),
                                       defaultZoneOffset,
                                       pattern);
    }

    static TimestampParserJava of(final DateTimeFormatter formatter, final ZoneOffset defaultZoneOffset) {
        return new TimestampParserJava(formatter, defaultZoneOffset, "");
    }

    @Deprecated
    @Override
    public org.joda.time.DateTimeZone getDefaultTimeZone() {
        return TimeZoneIds.convertZoneOffsetToJodaDateTimeZone(this.defaultZoneOffset);
    }

    @Override
    Instant parseInternal(final String text) throws TimestampParseException {
        final TemporalAccessor temporal;
        try {
            temporal = this.formatter.parse(text);
        } catch (DateTimeParseException ex) {
            throw new TimestampParseException(ex);
        }

        return this.buildOffsetDateTime(temporal).toInstant();
    }

    private OffsetDateTime buildOffsetDateTime(final TemporalAccessor given) throws TimestampParseException {
        // Non-offset (string-ish) time zone IDs are intentionally rejected by TimestampParserJava
        // because their meanings are not stable per the tz database.
        final ZoneId givenZoneId = given.query(TemporalQueries.zoneId());
        if (givenZoneId != null) {
            throw new TimestampParseException("Non-offset zone IDs are unaccepted in 'java:' formats: " + this.pattern);
        }

        final ZoneOffset givenZoneOffset = given.query(TemporalQueries.offset());
        if (given.isSupported(ChronoField.EPOCH_DAY) && given.isSupported(ChronoField.NANO_OF_DAY)) {
            // Normal fastest cases: Embulk expects the record has full date and time information in most cases.
            if (givenZoneOffset != null) {
                return OffsetDateTime.from(given);
            } else {
                return OffsetDateTime.of(LocalDateTime.from(given), this.defaultZoneOffset);
            }
        }

        // Exceptional cases: the record does not have full date and time information.
        final LocalDate dateFromDefault;
        if (given.isSupported(ChronoField.EPOCH_DAY)) {
            dateFromDefault = LocalDate.from(given);
        } else {
            final int year;
            if (given.isSupported(ChronoField.YEAR)) {
                year = given.get(ChronoField.YEAR);
            } else if (given.isSupported(ChronoField.YEAR_OF_ERA)) {
                year = given.get(ChronoField.YEAR_OF_ERA);
            } else {
                year = 1970;
            }

            if (given.isSupported(ChronoField.DAY_OF_YEAR)) {
                dateFromDefault = LocalDate.ofYearDay(year, given.get(ChronoField.DAY_OF_YEAR));
            } else {
                final int month;
                if (given.isSupported(ChronoField.MONTH_OF_YEAR)) {
                    month = given.get(ChronoField.MONTH_OF_YEAR);
                } else {
                    month = 1;
                }
                final int dayOfMonth;
                if (given.isSupported(ChronoField.DAY_OF_MONTH)) {
                    dayOfMonth = given.get(ChronoField.DAY_OF_MONTH);
                } else {
                    dayOfMonth = 1;
                }
                dateFromDefault = LocalDate.of(year, month, dayOfMonth);
            }
        }

        final LocalTime timeFromDefault;
        if (given.isSupported(ChronoField.NANO_OF_DAY)) {
            timeFromDefault = LocalTime.from(given);
        } else {
            if (given.isSupported(ChronoField.NANO_OF_DAY)) {
                timeFromDefault = LocalTime.ofNanoOfDay(given.getLong(ChronoField.NANO_OF_DAY));
            } else if (given.isSupported(ChronoField.MILLI_OF_DAY)) {
                timeFromDefault = LocalTime.ofNanoOfDay(given.getLong(ChronoField.MILLI_OF_DAY) * 1000000L);
            } else if (given.isSupported(ChronoField.SECOND_OF_DAY)) {
                timeFromDefault = LocalTime.ofSecondOfDay(given.getLong(ChronoField.SECOND_OF_DAY));
            } else if (given.isSupported(ChronoField.MINUTE_OF_DAY)) {
                timeFromDefault = LocalTime.ofSecondOfDay(given.getLong(ChronoField.MINUTE_OF_DAY) * 60L);
            } else {
                final int hourOfDay;
                if (given.isSupported(ChronoField.HOUR_OF_DAY)) {
                    hourOfDay = given.get(ChronoField.HOUR_OF_DAY);
                } else if (given.isSupported(ChronoField.CLOCK_HOUR_OF_DAY)) {
                    hourOfDay = given.get(ChronoField.CLOCK_HOUR_OF_DAY) % 24;
                } else if (given.isSupported(ChronoField.AMPM_OF_DAY)) {
                    final int ampmOfDay = given.get(ChronoField.AMPM_OF_DAY) * 12;
                    if (given.isSupported(ChronoField.HOUR_OF_AMPM)) {
                        hourOfDay = given.get(ChronoField.HOUR_OF_AMPM) + ampmOfDay;
                    } else if (given.isSupported(ChronoField.CLOCK_HOUR_OF_AMPM)) {
                        hourOfDay = given.get(ChronoField.CLOCK_HOUR_OF_AMPM) + ampmOfDay;
                    } else {
                        hourOfDay = 0;
                    }
                } else {
                    hourOfDay = 0;
                }
                final int minuteOfHour;
                if (given.isSupported(ChronoField.MINUTE_OF_HOUR)) {
                    minuteOfHour = given.get(ChronoField.MINUTE_OF_HOUR);
                } else {
                    minuteOfHour = 0;
                }
                final int secondOfMinute;
                if (given.isSupported(ChronoField.SECOND_OF_MINUTE)) {
                    secondOfMinute = given.get(ChronoField.SECOND_OF_MINUTE);
                } else {
                    secondOfMinute = 0;
                }
                final int nanoOfSecond;
                if (given.isSupported(ChronoField.NANO_OF_SECOND)) {
                    nanoOfSecond = given.get(ChronoField.NANO_OF_SECOND);
                } else if (given.isSupported(ChronoField.MICRO_OF_SECOND)) {
                    nanoOfSecond = given.get(ChronoField.MICRO_OF_SECOND) * 1000;
                } else if (given.isSupported(ChronoField.MILLI_OF_SECOND)) {
                    nanoOfSecond = given.get(ChronoField.MILLI_OF_SECOND) * 1000000;
                } else {
                    nanoOfSecond = 0;
                }
                timeFromDefault = LocalTime.of(hourOfDay, minuteOfHour, secondOfMinute, nanoOfSecond);
            }
        }

        if (givenZoneOffset != null) {
            return OffsetDateTime.of(dateFromDefault, timeFromDefault, givenZoneOffset);
        } else {
            return OffsetDateTime.of(dateFromDefault, timeFromDefault, this.defaultZoneOffset);
        }
    }

    private final DateTimeFormatter formatter;
    private final String pattern;
    private final ZoneOffset defaultZoneOffset;
}
