package org.embulk.spi.time;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import java.text.SimpleDateFormat;  // For default year/month/day if absent
import java.text.ParseException;  // For default year/month/day if absent
import java.util.Calendar;  // For default year/month/day if absent
import java.util.Date;  // For default year/month/day if absent
import java.util.List;
import java.util.Locale;  // For default year/month/day if absent
import java.util.TimeZone;  // For default year/month/day if absent
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;  // For default year/month/day if absent
import org.embulk.config.ConfigInject;

import static com.google.common.base.Strings.isNullOrEmpty;

public class TimestampParser
{
    public interface Task
    {
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        public String getDefaultTimeZoneId();

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

    public interface TimestampColumnOption
    {
        @Config("timezone")
        @ConfigDefault("null")
        public Optional<String> getTimeZoneId();

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

    private final org.joda.time.DateTimeZone defaultTimeZone;
    private final String formatString;
    private final RubyTimeParser parser;
    private final Calendar calendar;
    private final RubyTimeFormat format;

    @VisibleForTesting
    static TimestampParser createTimestampParserForTesting(Task task)
    {
        return new TimestampParser(task.getDefaultTimestampFormat(), task.getDefaultTimeZone(), task.getDefaultDate());
    }

    public TimestampParser(Task task, TimestampColumnOption columnOption)
    {
        this(
                columnOption.getFormat().or(task.getDefaultTimestampFormat()),
                columnOption.getTimeZone().or(task.getDefaultTimeZone()),
                columnOption.getDate().or(task.getDefaultDate()));
    }

    public TimestampParser(String formatString, org.joda.time.DateTimeZone defaultTimeZone)
    {
        this(formatString, defaultTimeZone, "1970-01-01");
    }

    public TimestampParser(final String formatString,
                           final org.joda.time.DateTimeZone defaultTimeZone,
                           final String defaultDate)
    {
        // TODO get default current time from ExecTask.getExecTimestamp
        this.formatString = formatString;
        this.format = RubyTimeFormat.compile(formatString);
        this.parser = new RubyTimeParser(format);
        this.defaultTimeZone = defaultTimeZone;

        // calculate default date
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date utc;
        try {
            utc = df.parse(defaultDate);
        }
        catch (ParseException ex) {
            throw new ConfigException("Invalid date format. Expected yyyy-MM-dd: " + defaultDate);
        }
        this.calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        this.calendar.setTime(utc);
    }

    public org.joda.time.DateTimeZone getDefaultTimeZone()
    {
        return defaultTimeZone;
    }

    public Timestamp parse(String text) throws TimestampParseException
    {
        if (isNullOrEmpty(text)) {
            throw new TimestampParseException("text is null or empty string.");
        }

        final TimeParseResult parseResult = parser.parse(text);
        if (parseResult == null) {
            throw new TimestampParseException("Cannot parse '" + text + "' by '" + formatString + "'");
        }
        return parseResult.toTimestamp(this.calendar.get(Calendar.YEAR),
                                       this.calendar.get(Calendar.MONTH) + 1,
                                       this.calendar.get(Calendar.DAY_OF_MONTH),
                                       this.defaultTimeZone);
    }
}
