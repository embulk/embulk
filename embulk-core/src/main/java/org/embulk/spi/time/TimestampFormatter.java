package org.embulk.spi.time;

import com.google.common.base.Optional;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.deps.timestamp.DepsTimestampFormatter;
import org.embulk.spi.util.LineEncoder;

@Deprecated  // Externalized to embulk-util-timestamp: https://github.com/embulk/embulk/issues/1298
public class TimestampFormatter {
    private TimestampFormatter(final String pattern, final String zoneIdString) {
        this.delegate = DepsTimestampFormatter.of(pattern, utcToNull(pattern, zoneIdString));
        this.zoneIdString = zoneIdString;
    }

    @Deprecated  // This constructor will be removed sooner when Joda-Time is removed from Embulk during v0.10.
    public TimestampFormatter(final Task task, final Optional<? extends TimestampColumnOption> columnOption) {
        this(columnOption.isPresent()
                     ? columnOption.get().getFormat().or(task.getDefaultTimestampFormat())
                     : task.getDefaultTimestampFormat(),
             columnOption.isPresent()
                     ? columnOption.get().getTimeZoneId().or(task.getDefaultTimeZoneId())
                     : task.getDefaultTimeZoneId());
    }

    @Deprecated  // This constructor will be removed sooner when Joda-Time is removed from Embulk during v0.10.
    public TimestampFormatter(final String format, final org.joda.time.DateTimeZone timeZone) {
        this(format, timeZone.toTimeZone().toZoneId().toString());
    }

    public static TimestampFormatter of(final String pattern, final String zoneIdString) {
        return new TimestampFormatter(pattern, zoneIdString);
    }

    @Deprecated  // This constructor will be removed sooner when Joda-Time is removed from Embulk during v0.10.
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

        return new TimestampFormatter(pattern, zoneIdString);
    }

    @Deprecated  // This interface will be removed sooner when Joda-Time is removed from Embulk during v0.10.
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

    @Deprecated  // This interface will be removed sooner when Joda-Time is removed from Embulk during v0.10.
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

    @Deprecated  // This method will be removed sooner when Joda-Time is removed from Embulk during v0.10.
    public org.joda.time.DateTimeZone getTimeZone() {
        return TimeZoneIds.parseJodaDateTimeZone(this.zoneIdString);
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public String format(final Timestamp value) {
        // It may throw DateTimeException.
        return this.delegate.format(value.getInstant());
    }

    // Receiving LineEncoder as a parameter is deprecated. TimestampFormatter should have fewer dependencies inside.
    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public final void format(final Timestamp value, final LineEncoder encoder) {
        // TODO: Optimize by directly appending to internal buffer
        encoder.addText(this.format(value));
    }

    private static String utcToNull(final String pattern, final String zoneIdString) {
        if (pattern.startsWith("java:") || pattern.startsWith("ruby:")) {
            if (zoneIdString == null || zoneIdString.equals("UTC")) {
                return null;
            }
        }
        return zoneIdString;
    }

    private final DepsTimestampFormatter delegate;

    private final String zoneIdString;  // Saved only for deprecated #getTimeZone().
}
