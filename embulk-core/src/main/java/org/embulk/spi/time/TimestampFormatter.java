package org.embulk.spi.time;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.spi.util.LineEncoder;

@Deprecated  // Externalized to embulk-util-timestamp: https://github.com/embulk/embulk/issues/1298
public class TimestampFormatter {
    private TimestampFormatter(final String pattern, final String zoneIdString) {
        this.delegate = TimestampFormatterDelegate.of(pattern, utcToNull(pattern, zoneIdString));
        this.zoneIdString = zoneIdString;
    }

    // The constructor has been removed:
    // public TimestampFormatter(TimestampFormatter.Task, com.google.commom.base.Optional<? extends TimestampFormatter.TimestampColumnOption>)

    public static TimestampFormatter of(final String pattern, final String zoneIdString) {
        return new TimestampFormatter(pattern, zoneIdString);
    }

    // The method has been removed:
    // public static TimestampFormatter of(TimestampFormatter.Task, com.google.common.base.Optional<? extends TimestampFormatter.TimestampColumnOption>)

    @Deprecated  // This interface will be removed sooner when Joda-Time is removed from Embulk during v0.10.
    public interface Task {
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        public String getDefaultTimeZoneId();

        // The method has been removed: public default org.joda.time.DateTimeZone getDefaultTimeZone()

        @Config("default_timestamp_format")
        @ConfigDefault("\"%Y-%m-%d %H:%M:%S.%6N %z\"")
        public String getDefaultTimestampFormat();
    }

    // The interface has been removed: public interface TimestampFormatter.TimestampColumnOption

    // The method has been removed: public org.joda.time.DateTimeZone getTimeZone()

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

    private final TimestampFormatterDelegate delegate;

    private final String zoneIdString;  // Saved only for deprecated #getTimeZone().
}
