package org.embulk.spi.time;

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

    // The constructor has been removed: public TimestampParser(TimestampParser.Task, TimestampParser.TimestampColumnOption)

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

    // The method has been removed: public static TimestampParser of(TimestampParser.Task, TimestampParser.TimestampColumnOption)

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

    // The interface has been removed: public interface TimestampParser.TimestampColumnOption

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
