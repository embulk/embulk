package org.embulk.spi.time;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.deps.timestamp.TimestampFormatter;

@Deprecated
public class TimestampParser {
    private TimestampParser(final org.embulk.deps.timestamp.TimestampFormatter formatter,
                            final org.joda.time.DateTimeZone defaultJodaDateTimeZone) {
        this.formatter = formatter;
        this.defaultJodaDateTimeZone = defaultJodaDateTimeZone;
    }

    // Calling the constructor directly is deprecated, but the constructor is kept for plugin compatibility.
    // Use TimestampParser.of(Task, TimestampColumnOption) instead.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public TimestampParser(final Task task,
                           final TimestampColumnOption columnOption) {
        this(org.embulk.deps.timestamp.TimestampFormatter.createLegacy(
                     columnOption.getFormat().or(task.getDefaultTimestampFormat()),
                     columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId()),
                     columnOption.getDate().or(task.getDefaultDate())),
             JodaTimeCompat.parseJodaDateTimeZone(columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId())));
    }

    // Using Joda-Time is deprecated, but the constructor receives org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public TimestampParser(final String formatString,
                           final org.joda.time.DateTimeZone defaultJodaDateTimeZone) {
        this(org.embulk.deps.timestamp.TimestampFormatter.createLegacy(
                     formatString,
                     JodaTimeCompat.convertJodaDateTimeZoneToZoneIdString(defaultJodaDateTimeZone),
                     "1970-01-01"),
             defaultJodaDateTimeZone);
    }

    // Using Joda-Time is deprecated, but the constructor receives org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public TimestampParser(final String formatString,
                           final org.joda.time.DateTimeZone defaultJodaDateTimeZone,
                           final String defaultDate) {
        this(org.embulk.deps.timestamp.TimestampFormatter.createLegacy(
                     formatString,
                     JodaTimeCompat.convertJodaDateTimeZoneToZoneIdString(defaultJodaDateTimeZone),
                     defaultDate),
             defaultJodaDateTimeZone);
    }

    // "default_date" is deprecated, but the creator method is kept for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public static TimestampParser of(final String pattern,
                                     final String defaultZoneIdString,
                                     final String defaultDateString) {
        final org.embulk.deps.timestamp.TimestampFormatter delegate = org.embulk.deps.timestamp.TimestampFormatter.createLegacy(
                pattern, defaultZoneIdString, defaultDateString);
        return new TimestampParser(delegate, JodaTimeCompat.parseJodaDateTimeZone(defaultZoneIdString));
    }

    @Deprecated
    public static TimestampParser of(final String pattern,
                                     final String defaultZoneIdString) {
        final org.embulk.deps.timestamp.TimestampFormatter delegate = org.embulk.deps.timestamp.TimestampFormatter.createLegacy(
                pattern, defaultZoneIdString, "1970-01-01");
        return new TimestampParser(delegate, JodaTimeCompat.parseJodaDateTimeZone(defaultZoneIdString));
    }

    @Deprecated
    public static TimestampParser of(final Task task,
                                     final TimestampColumnOption columnOption) {
        final org.embulk.deps.timestamp.TimestampFormatter delegate = org.embulk.deps.timestamp.TimestampFormatter.createLegacy(
                columnOption.getFormat().or(task.getDefaultTimestampFormat()),
                columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId()),
                columnOption.getDate().or(task.getDefaultDate()));
        return new TimestampParser(
                delegate, JodaTimeCompat.parseJodaDateTimeZone(columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId())));
    }

    @Deprecated
    public interface Task {
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        public String getDefaultTimeZoneId();

        // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
        // It won't be removed very soon at least until Embulk v0.10.
        @Deprecated
        public default org.joda.time.DateTimeZone getDefaultTimeZone() {
            if (getDefaultTimeZoneId() != null) {
                return JodaTimeCompat.parseJodaDateTimeZone(getDefaultTimeZoneId());
            } else {
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

    @Deprecated
    public interface TimestampColumnOption {
        @Config("timezone")
        @ConfigDefault("null")
        public com.google.common.base.Optional<String> getTimeZoneId();

        // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
        // It won't be removed very soon at least until Embulk v0.10.
        @Deprecated
        public default com.google.common.base.Optional<org.joda.time.DateTimeZone> getTimeZone() {
            if (getTimeZoneId().isPresent()) {
                return com.google.common.base.Optional.of(JodaTimeCompat.parseJodaDateTimeZone(getTimeZoneId().get()));
            } else {
                return com.google.common.base.Optional.absent();
            }
        }

        @Config("format")
        @ConfigDefault("null")
        public com.google.common.base.Optional<String> getFormat();

        @Config("date")
        @ConfigDefault("null")
        public com.google.common.base.Optional<String> getDate();
    }

    // Using Joda-Time is deprecated, but the method return org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public org.joda.time.DateTimeZone getDefaultTimeZone() {
        return this.defaultJodaDateTimeZone;
    }

    @Deprecated
    public final Timestamp parse(final String text) throws TimestampParseException {
        try {
            return Timestamp.ofInstant(this.formatter.parse(text));
        } catch (final DateTimeException ex) {
            throw new TimestampParseException(ex);
        }
    }

    private final org.embulk.deps.timestamp.TimestampFormatter formatter;
    private final org.joda.time.DateTimeZone defaultJodaDateTimeZone;
}
