package org.embulk.spi.time;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import org.joda.time.DateTimeZone;
import com.google.common.base.Optional;
import org.jruby.embed.ScriptingContainer;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
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

    private final JRubyTimeParserHelper helper;
    private final DateTimeZone defaultTimeZone;

    @Deprecated
    public TimestampParser(String format, ParserTask task)
    {
        this(task.getJRuby(), format, task.getDefaultTimeZone());
    }

    TimestampParser(Task task)
    {
        this(task.getJRuby(), task.getDefaultTimestampFormat(), task.getDefaultTimeZone(), task.getDefaultDate());
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
        JRubyTimeParserHelperFactory helperFactory = (JRubyTimeParserHelperFactory) jruby.runScriptlet("Embulk::Java::TimeParserHelper::Factory.new");

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
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        cal.setTime(utc);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        // TODO get default current time from ExecTask.getExecTimestamp
        this.helper = (JRubyTimeParserHelper) helperFactory.newInstance(format, year, month, day, 0, 0, 0, 0);  // TODO default time zone
        this.defaultTimeZone = defaultTimeZone;
    }

    public DateTimeZone getDefaultTimeZone()
    {
        return defaultTimeZone;
    }

    public Timestamp parse(String text) throws TimestampParseException
    {
        long localUsec = helper.strptimeUsec(text);
        String zone = helper.getZone();

        DateTimeZone timeZone = defaultTimeZone;
        if (zone != null) {
            // TODO cache parsed zone?
            timeZone = parseDateTimeZone(zone);
            if (timeZone == null) {
                throw new TimestampParseException("Invalid time zone name '" + text + "'");
            }
        }

        long localSec = localUsec / 1000000;
        long usec = localUsec % 1000000;
        long sec = timeZone.convertLocalToUTC(localSec*1000, false) / 1000;

        return Timestamp.ofEpochSecond(sec, usec * 1000);
    }
}
