package org.embulk.spi.time;

import com.google.common.base.Optional;
import java.time.ZoneOffset;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.spi.util.LineEncoder;

public class TimestampFormatter {
    private TimestampFormatter(final TimestampFormatterRuby delegate) {
        this.delegate = delegate;
    }

    // Constructor to be called from subclasses such as TimestampFormatterRuby.
    TimestampFormatter() {
        this.delegate = null;
    }

    // Calling the constructor directly is deprecated, but the constructor is kept for plugin compatibility.
    // Use TimestampFormatter.of(Task, TimestampColumnOption) instead.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public TimestampFormatter(final Task task, final Optional<? extends TimestampColumnOption> columnOption) {
        this(TimestampFormatterRuby.ofLegacy(
                     columnOption.isPresent()
                             ? columnOption.get().getFormat().or(task.getDefaultTimestampFormat())
                             : task.getDefaultTimestampFormat(),
                     columnOption.isPresent()
                             ? columnOption.get().getTimeZone().or(task.getDefaultTimeZone())
                             : task.getDefaultTimeZone()));
    }

    // Using Joda-Time is deprecated, but the constructor receives org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public TimestampFormatter(final String format, final org.joda.time.DateTimeZone timeZone) {
        this(TimestampFormatterRuby.ofLegacy(format, timeZone));
    }

    public static TimestampFormatter of(final String pattern, final String zoneIdString) {
        if (pattern.startsWith("java:")) {
            final ZoneOffset zoneOffset;
            if (zoneIdString.equals("UTC")) {
                zoneOffset = ZoneOffset.UTC;
            } else {
                zoneOffset = ZoneOffset.of(zoneIdString);
            }
            return TimestampFormatterJava.of(pattern.substring(5), zoneOffset);
        } else if (pattern.startsWith("ruby:")) {
            final ZoneOffset zoneOffset;
            if (zoneIdString.equals("UTC")) {
                zoneOffset = ZoneOffset.UTC;
            } else {
                zoneOffset = ZoneOffset.of(zoneIdString);
            }
            return TimestampFormatterRuby.of(pattern.substring(5), zoneOffset);
        } else {
            return TimestampFormatterRuby.ofLegacy(pattern, TimeZoneIds.parseJodaDateTimeZone(zoneIdString));
        }
    }

    public static TimestampFormatter of(final Task task, final Optional<? extends TimestampColumnOption> columnOption) {
        final String pattern;
        if (columnOption.isPresent()) {
            pattern = columnOption.get().getFormat().or(task.getDefaultTimestampFormat());
        } else {
            pattern = task.getDefaultTimestampFormat();
        }

        final String zoneIdString;
        if (columnOption.isPresent()) {
            zoneIdString = columnOption.get().getTimeZoneId().or(task.getDefaultTimeZoneId());
        } else {
            zoneIdString = task.getDefaultTimeZoneId();
        }

        if (pattern.startsWith("java:")) {
            final ZoneOffset zoneOffset;
            if (zoneIdString.equals("UTC")) {
                zoneOffset = ZoneOffset.UTC;
            } else {
                zoneOffset = ZoneOffset.of(zoneIdString);
            }
            return TimestampFormatterJava.of(pattern.substring(5), zoneOffset);
        } else if (pattern.startsWith("ruby:")) {
            final ZoneOffset zoneOffset;
            if (zoneIdString.equals("UTC")) {
                zoneOffset = ZoneOffset.UTC;
            } else {
                zoneOffset = ZoneOffset.of(zoneIdString);
            }
            return TimestampFormatterRuby.of(pattern.substring(5), zoneOffset);
        } else {
            return TimestampFormatterRuby.ofLegacy(pattern,
                                                   TimeZoneIds.parseJodaDateTimeZone(zoneIdString));
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
            } else {
                return null;
            }
        }

        @Config("default_timestamp_format")
        @ConfigDefault("\"%Y-%m-%d %H:%M:%S.%6N %z\"")
        public String getDefaultTimestampFormat();
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
            } else {
                return Optional.absent();
            }
        }

        @Config("format")
        @ConfigDefault("null")
        public Optional<String> getFormat();
    }

    // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public org.joda.time.DateTimeZone getTimeZone() {
        if (this.delegate == null) {
            throw new RuntimeException("FATAL: Unexpected execution path of TimestampFormatter without delegate.");
        }
        return this.delegate.getTimeZone();
    }

    public String format(final Timestamp value) {
        if (this.delegate == null) {
            throw new RuntimeException("FATAL: Unexpected execution path of TimestampFormatter without delegate.");
        }
        return this.delegate.format(value);
    }

    // Receiving LineEncoder as a parameter is deprecated. TimestampFormatter should have fewer dependencies inside.
    // It won't be removed very soon at least until Embulk v0.10.
    @Deprecated
    public final void format(final Timestamp value, final LineEncoder encoder) {
        // TODO: Optimize by directly appending to internal buffer
        encoder.addText(this.format(value));
    }

    private final TimestampFormatterRuby delegate;
}
