package org.embulk.spi.time;

import com.google.common.base.Optional;
import java.time.DateTimeException;
import java.time.Instant;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.deps.timestamp.DepsTimestampFormatter;

@Deprecated  // Externalized to embulk-util-timestamp: https://github.com/embulk/embulk/issues/1298
public class TimestampParser {
    private TimestampParser(final String formatString, final String defaultZoneIdString, final String defaultDateString) {
        this.delegate = DepsTimestampFormatter.of(formatString, utcToNull(formatString, defaultZoneIdString), defaultDateString);
        this.defaultZoneIdString = defaultZoneIdString;
    }

    private TimestampParser(final String formatString, final String defaultZoneIdString) {
        this(formatString, defaultZoneIdString, null);
    }

    @Deprecated  // This constructor will be removed sooner when Joda-Time is removed from Embulk during v0.10.
    public TimestampParser(final Task task,
                           final TimestampColumnOption columnOption) {
        this(columnOption.getFormat().or(task.getDefaultTimestampFormat()),
             columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId()),
             columnOption.getDate().or(task.getDefaultDate()));
    }

    @Deprecated  // "default_date" is deprecated, but the creator method is kept for plugin compatibility.
    public static TimestampParser of(final String pattern,
                                     final String defaultZoneIdString,
                                     final String defaultDateString) {
        return new TimestampParser(pattern, defaultZoneIdString, defaultDateString);
    }

    public static TimestampParser of(final String pattern,
                                     final String defaultZoneIdString) {
        // embulk-util-timestamp's default date is 1970-01-01.
        return new TimestampParser(pattern, defaultZoneIdString);
    }

    @Deprecated  // This constructor will be removed sooner when Joda-Time is removed from Embulk during v0.10.
    public static TimestampParser of(final Task task,
                                     final TimestampColumnOption columnOption) {
        final String pattern = columnOption.getFormat().or(task.getDefaultTimestampFormat());
        return new TimestampParser(
                    pattern,
                    columnOption.getTimeZoneId().or(task.getDefaultTimeZoneId()),
                    columnOption.getDate().or(task.getDefaultDate()));
    }

    @Deprecated  // This interface will be removed sooner when Joda-Time is removed from Embulk during v0.10.
    public interface Task {
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        public String getDefaultTimeZoneId();

        // The method has been removed: public default org.joda.time.DateTimeZone getDefaultTimeZone()

        @Config("default_timestamp_format")
        @ConfigDefault("\"%Y-%m-%d %H:%M:%S.%N %z\"")
        public String getDefaultTimestampFormat();

        @Config("default_date")
        @ConfigDefault("\"1970-01-01\"")
        public String getDefaultDate();
    }

    @Deprecated  // This interface will be removed sooner when Joda-Time is removed from Embulk during v0.10.
    public interface TimestampColumnOption {
        @Config("timezone")
        @ConfigDefault("null")
        public Optional<String> getTimeZoneId();

        // The method has been removed: public default Optional<org.joda.time.DateTimeZone> getTimeZone()

        @Config("format")
        @ConfigDefault("null")
        public Optional<String> getFormat();

        @Config("date")
        @ConfigDefault("null")
        public Optional<String> getDate();
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public final Timestamp parse(final String text) throws TimestampParseException {
        final Instant instant;
        try {
            instant = this.delegate.parse(text);
        } catch (final DateTimeException ex) {
            throw new TimestampParseException(ex);
        }
        return Timestamp.ofInstant(instant);
    }

    private static String utcToNull(final String formatString, final String defaultZoneIdString) {
        if (formatString.startsWith("java:") || formatString.startsWith("ruby:")) {
            if (defaultZoneIdString == null || defaultZoneIdString.equals("UTC")) {
                return null;
            }
        }
        return defaultZoneIdString;
    }

    private final DepsTimestampFormatter delegate;

    private final String defaultZoneIdString;  // Saved only for deprecated #getTimeZone().
}
