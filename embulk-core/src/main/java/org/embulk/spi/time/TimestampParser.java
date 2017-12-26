package org.embulk.spi.time;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;  // For default year/month/day if absent

public class TimestampParser
{
    private TimestampParser(final String formatString,
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

    private TimestampParser(final String formatString,
                            final ZoneId defaultZoneId,
                            final org.joda.time.DateTimeZone defaultJodaDateTimeZone,
                            final LocalDate defaultDate) {
        this(formatString,
             defaultZoneId,
             defaultJodaDateTimeZone,
             defaultDate.getYear(),
             defaultDate.getMonthValue(),
             defaultDate.getDayOfMonth());
    }

    private TimestampParser(final String formatString,
                            final ZoneId defaultZoneId,
                            final org.joda.time.DateTimeZone defaultJodaDateTimeZone,
                            final String defaultDate) {
        this(formatString,
             defaultZoneId,
             defaultJodaDateTimeZone,
             parseDateForDefault(defaultDate));
    }

    public TimestampParser(final Task task,
                           final TimestampColumnOption columnOption) {
        this(columnOption.getFormat().or(task.getDefaultTimestampFormat()),
             TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab(
                 columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId())),
             TimeZoneIds.parseJodaDateTimeZone(
                 columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId())),
             columnOption.getDate().or(task.getDefaultDate()));
    }

    // Using Joda-Time is deprecated, but the constructor receives org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public TimestampParser(final String formatString,
                           final org.joda.time.DateTimeZone defaultJodaDateTimeZone) {
        this(formatString,
             TimeZoneIds.convertJodaDateTimeZoneToZoneId(defaultJodaDateTimeZone),
             defaultJodaDateTimeZone,
             1970,
             1,
             1);
    }

    // Using Joda-Time is deprecated, but the constructor receives org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public TimestampParser(final String formatString,
                           final org.joda.time.DateTimeZone defaultJodaDateTimeZone,
                           final String defaultDate) {
        this(formatString,
             TimeZoneIds.convertJodaDateTimeZoneToZoneId(defaultJodaDateTimeZone),
             defaultJodaDateTimeZone,
             defaultDate);
    }

    public static TimestampParser create(final String formatString,
                                         final String defaultTimeZoneId,
                                         final String defaultDate) {
        return new TimestampParser(formatString,
                                   TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab(defaultTimeZoneId),
                                   TimeZoneIds.parseJodaDateTimeZone(defaultTimeZoneId),
                                   defaultDate);
    }

    public static TimestampParser create(final String formatString,
                                         final String defaultTimeZoneId) {
        return new TimestampParser(formatString,
                                   TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab(defaultTimeZoneId),
                                   TimeZoneIds.parseJodaDateTimeZone(defaultTimeZoneId),
                                   1970,
                                   1,
                                   1);
    }

    @VisibleForTesting
    static TimestampParser createTimestampParserForTesting(final Task task) {
        return new TimestampParser(task.getDefaultTimestampFormat(), task.getDefaultTimeZone(), task.getDefaultDate());
    }

    public interface Task {
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        public String getDefaultTimeZoneId();

        // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
        // It won't be removed very soon at least until Embulk v0.10.
        @Deprecated
        public default org.joda.time.DateTimeZone getDefaultTimeZone() {
            if (getDefaultTimeZoneId() != null) {
                return TimestampFormat.parseDateTimeZone(getDefaultTimeZoneId());
            }
            else {
                return null;
            }
        }

        @Config("default_timestamp_format")
        @ConfigDefault("\"%Y-%m-%d %H:%M:%S.%N %z\"")
        public String getDefaultTimestampFormat();

        @Config("default_date")
        @ConfigDefault("\"1970-01-01\"")
        public String getDefaultDate();
    }

    public interface TimestampColumnOption {
        @Config("timezone")
        @ConfigDefault("null")
        public Optional<String> getTimeZoneId();

        // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
        // It won't be removed very soon at least until Embulk v0.10.
        @Deprecated
        public default Optional<org.joda.time.DateTimeZone> getTimeZone() {
            if (getTimeZoneId().isPresent()) {
                return Optional.of(TimestampFormat.parseDateTimeZone(getTimeZoneId().get()));
            }
            else {
                return Optional.absent();
            }
        }

        @Config("format")
        @ConfigDefault("null")
        public Optional<String> getFormat();

        @Config("date")
        @ConfigDefault("null")
        public Optional<String> getDate();
    }

    // Using Joda-Time is deprecated, but the method return org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public org.joda.time.DateTimeZone getDefaultTimeZone() {
        return defaultJodaDateTimeZone;
    }

    public Timestamp parse(String text) throws TimestampParseException {
        if (isNullOrEmpty(text)) {
            throw new TimestampParseException("text is null or empty string.");
        }

        final TimeParsed parseResult = parser.parse(text);
        if (parseResult == null) {
            throw new TimestampParseException("Cannot parse '" + text + "' by '" + formatString + "'");
        }
        return parseResult.toTimestampLegacy(this.defaultYear,
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
