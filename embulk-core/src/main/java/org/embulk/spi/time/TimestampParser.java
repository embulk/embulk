package org.embulk.spi.time;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.jruby.embed.ScriptingContainer;
import org.embulk.spi.time.StrptimeParser.FormatBag;

import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.embulk.spi.time.TimestampFormat.parseDateTimeZone;

public class TimestampParser
{
    @Deprecated
    public interface ParserTask
            extends org.embulk.config.Task
    {
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        public DateTimeZone getDefaultTimeZone();

        @ConfigInject
        public ScriptingContainer getJRuby();
    }

    public interface Task
    {
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        public DateTimeZone getDefaultTimeZone();

        @Config("default_timestamp_format")
        @ConfigDefault("\"%Y-%m-%d %H:%M:%S.%N %z\"")
        public String getDefaultTimestampFormat();

        @Config("default_date")
        @ConfigDefault("\"1970-01-01\"")
        public String getDefaultDate();

        @ConfigInject
        public ScriptingContainer getJRuby();
    }

    public interface TimestampColumnOption
    {
        @Config("timezone")
        @ConfigDefault("null")
        public Optional<DateTimeZone> getTimeZone();

        @Config("format")
        @ConfigDefault("null")
        public Optional<String> getFormat();

        @Config("date")
        @ConfigDefault("null")
        public Optional<String> getDate();
    }

    private final DateTimeZone defaultTimeZone;
    private final String format;
    private final StrptimeParser parser;
    private final Calendar calendar;
    private final List<StrptimeToken> compiledPattern;

    @Deprecated
    public TimestampParser(String format, ParserTask task)
    {
        this(task.getJRuby(), format, task.getDefaultTimeZone());
    }

    @VisibleForTesting
    static TimestampParser createTimestampParserForTesting(Task task)

    {
        return new TimestampParser(task.getJRuby(), task.getDefaultTimestampFormat(), task.getDefaultTimeZone(), task.getDefaultDate());
    }

    public TimestampParser(Task task, TimestampColumnOption columnOption)
    {
        this(task.getJRuby(),
                columnOption.getFormat().or(task.getDefaultTimestampFormat()),
                columnOption.getTimeZone().or(task.getDefaultTimeZone()),
                columnOption.getDate().or(task.getDefaultDate()));
    }

    public TimestampParser(ScriptingContainer jruby, String format, DateTimeZone defaultTimeZone)
    {
        this(jruby, format, defaultTimeZone, "1970-01-01");
    }

    public TimestampParser(ScriptingContainer jruby, String format, DateTimeZone defaultTimeZone, String defaultDate)
    {
        // TODO get default current time from ExecTask.getExecTimestamp
        this.format = format;
        this.parser = new StrptimeParser();
        this.compiledPattern = this.parser.compilePattern(format);
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

    public DateTimeZone getDefaultTimeZone()
    {
        return defaultTimeZone;
    }

    public Timestamp parse(String text) throws TimestampParseException
    {
        if (isNullOrEmpty(text)) {
            throw new TimestampParseException("text is null or empty string.");
        }

        final FormatBag bag = parser.parse(compiledPattern, text);
        if (bag == null) {
            throw new TimestampParseException("Cannot parse '" + text + "' by '" + format + "'");
        }
        bag.setYearIfNotSet(calendar.get(Calendar.YEAR));
        bag.setMonthIfNotSet(calendar.get(Calendar.MONTH) + 1);
        bag.setMdayIfNotSet(calendar.get(Calendar.DAY_OF_MONTH));

        final LocalTime local = createLocalTimeFromFormatBag(bag);
        final String zone = local.getZone();
        final DateTimeZone timeZone;
        if (zone != null) {
            // TODO cache parsed zone?
            timeZone = parseDateTimeZone(zone);
            if (timeZone == null) {
                throw new TimestampParseException("Invalid time zone name '" + text + "'");
            }
        }
        else {
            timeZone = defaultTimeZone;
        }

        final long sec = timeZone.convertLocalToUTC(local.getSeconds() * 1000, false) / 1000;
        return Timestamp.ofEpochSecond(sec, local.getNsecFraction());
    }

    public LocalTime createLocalTimeFromFormatBag(FormatBag bag)
    {
        final long sec_fraction_nsec;
        if (FormatBag.has(bag.sec_fraction)) {
            sec_fraction_nsec = bag.sec_fraction * (int)Math.pow(10, 9 - bag.sec_fraction_size);
        }
        else {
            sec_fraction_nsec = 0;
        }

        final long sec;
        if (bag.hasSeconds()) {
            if (FormatBag.has(bag.seconds_size)) {
                sec = bag.seconds / (int)Math.pow(10, bag.seconds_size);
            }
            else { // int
                sec = bag.seconds;
            }

        } else {
            final int year;
            if (FormatBag.has(bag.year)) {
                year = bag.year;
            }
            else {
                year = 1970;
            }

            // set up with min this and then add to allow rolling over
            DateTime dt = new DateTime(year, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
            if (FormatBag.has(bag.mon)) {
                dt = dt.plusMonths(bag.mon - 1);
            }
            if (FormatBag.has(bag.mday)) {
                dt = dt.plusDays(bag.mday - 1);
            }
            if (FormatBag.has(bag.hour)) {
                dt = dt.plusHours(bag.hour);
            }
            if (FormatBag.has(bag.min)) {
                dt = dt.plusMinutes(bag.min);
            }
            if (FormatBag.has(bag.sec)) {
                dt = dt.plusSeconds(bag.sec);
            }
            sec = dt.getMillis() / 1000;
        }

        return new LocalTime(sec, sec_fraction_nsec, bag.zone);
    }

    private static class LocalTime
    {
        private final long seconds;
        private final long nsecFraction;
        private final String zone;  // +0900, JST, UTC

        public LocalTime(long seconds, long nsecFraction, String zone)
        {
            this.seconds = seconds;
            this.nsecFraction = nsecFraction;
            this.zone = zone;
        }

        public long getSeconds()
        {
            return seconds;
        }

        public long getNsecFraction()
        {
            return nsecFraction;
        }

        public String getZone()
        {
            return zone;
        }
    }
}
