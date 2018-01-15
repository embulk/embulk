package org.embulk.spi.time;

import com.google.common.base.Optional;
import java.time.Instant;
import java.time.ZoneOffset;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;

public class TimestampParser {
    private TimestampParser(final TimestampParserLegacy delegate) {
        this.delegate = delegate;
    }

    TimestampParser() {
        this(null);
    }

    // Calling the constructor directly is deprecated, but the constructor is kept for plugin compatibility.
    // Use TimestampParser.of(Task, TimestampColumnOption) instead.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public TimestampParser(final Task task,
                           final TimestampColumnOption columnOption) {
        this(TimestampParserLegacy.of(
                 columnOption.getFormat().or(task.getDefaultTimestampFormat()),
                 TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab(
                     columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId())),
                 TimeZoneIds.parseJodaDateTimeZone(
                     columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId())),
                 columnOption.getDate().or(task.getDefaultDate())));
    }

    // Using Joda-Time is deprecated, but the constructor receives org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public TimestampParser(final String formatString,
                           final org.joda.time.DateTimeZone defaultJodaDateTimeZone) {
        this(TimestampParserLegacy.of(
                 formatString,
                 TimeZoneIds.convertJodaDateTimeZoneToZoneId(defaultJodaDateTimeZone),
                 defaultJodaDateTimeZone,
                 1970,
                 1,
                 1));
    }

    // Using Joda-Time is deprecated, but the constructor receives org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public TimestampParser(final String formatString,
                           final org.joda.time.DateTimeZone defaultJodaDateTimeZone,
                           final String defaultDate) {
        this(TimestampParserLegacy.of(
                 formatString,
                 TimeZoneIds.convertJodaDateTimeZoneToZoneId(defaultJodaDateTimeZone),
                 defaultJodaDateTimeZone,
                 defaultDate));
    }

    // "default_date" is deprecated, but the creator method is kept for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public static TimestampParser of(final String pattern,
                                     final String defaultZoneIdString,
                                     final String defaultDateString) {
        if (pattern.startsWith("java:")) {
            // TODO: Warn if "default_date" is set, which is unavailable for "java:".
            final ZoneOffset zoneOffset;
            if (defaultZoneIdString.equals("UTC")) {
                zoneOffset = ZoneOffset.UTC;
            } else {
                zoneOffset = ZoneOffset.of(defaultZoneIdString);
            }
            return TimestampParserJava.of(pattern.substring(5),
                                          zoneOffset);
        } else {
            return TimestampParserLegacy.of(pattern,
                                            TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab(defaultZoneIdString),
                                            TimeZoneIds.parseJodaDateTimeZone(defaultZoneIdString),
                                            defaultDateString);
        }
    }

    public static TimestampParser of(final String pattern,
                                     final String defaultZoneIdString) {
        if (pattern.startsWith("java:")) {
            final ZoneOffset zoneOffset;
            if (defaultZoneIdString.equals("UTC")) {
                zoneOffset = ZoneOffset.UTC;
            } else {
                zoneOffset = ZoneOffset.of(defaultZoneIdString);
            }
            return TimestampParserJava.of(pattern.substring(5),
                                          zoneOffset);
        } else {
            return TimestampParserLegacy.of(pattern,
                                            TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab(defaultZoneIdString),
                                            TimeZoneIds.parseJodaDateTimeZone(defaultZoneIdString),
                                            1970,
                                            1,
                                            1);
        }
    }

    public static TimestampParser of(final Task task,
                                     final TimestampColumnOption columnOption) {
        final String pattern = columnOption.getFormat().or(task.getDefaultTimestampFormat());
        if (pattern.startsWith("java:")) {
            // TODO: Warn if "default_date" is set, which is unavailable for "java:".
            final String zoneOffsetString = columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId());
            final ZoneOffset zoneOffset;
            if (zoneOffsetString.equals("UTC")) {
                zoneOffset = ZoneOffset.UTC;
            } else {
                zoneOffset = ZoneOffset.of(zoneOffsetString);
            }
            return TimestampParserJava.of(pattern.substring(5),
                                          zoneOffset);
        } else {
            return TimestampParserLegacy.of(pattern,
                                            TimeZoneIds.parseZoneIdWithJodaAndRubyZoneTab(
                                                columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId())),
                                            TimeZoneIds.parseJodaDateTimeZone(
                                                columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId())),
                                            columnOption.getDate().or(task.getDefaultDate()));
        }
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
                return TimeZoneIds.parseJodaDateTimeZone(getDefaultTimeZoneId());
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
                return Optional.of(TimeZoneIds.parseJodaDateTimeZone(getTimeZoneId().get()));
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
        if (this.delegate == null) {
            throw new RuntimeException("FATAL: Unexpected execution path of TimestampParser without delegate.");
        }
        return this.delegate.getDefaultTimeZone();
    }

    Instant parseInternal(final String text) throws TimestampParseException {
        if (this.delegate == null) {
            throw new TimestampParseException("FATAL: Unexpected execution path of TimestampParser without delegate.");
        }
        return this.delegate.parseInternal(text);
    }

    public final Timestamp parse(final String text) throws TimestampParseException {
        return Timestamp.ofInstant(this.parseInternal(text));
    }

    private final TimestampParserLegacy delegate;
}
