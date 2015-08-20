package org.embulk.spi.time;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar;
import com.google.common.base.Strings;
import com.google.common.base.Optional;
import org.joda.time.DateTimeZone;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.jruby.Ruby;
import org.jruby.embed.ScriptingContainer;
import org.jruby.util.RubyDateFormatter;
import org.jruby.util.RubyDateParser;
import org.jruby.util.RubyDateParser.FormatBag;
import org.jruby.util.RubyDateParser.LocalTime;

import java.util.List;

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
    private final RubyDateParser parser;
    private final Calendar cal;
    private final List<RubyDateFormatter.Token> compiledPattern;

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
        // TODO get default current time from ExecTask.getExecTimestamp
        Ruby runtime = jruby.getProvider().getRuntime();
        this.format = format;
        this.parser = new RubyDateParser(runtime.getCurrentContext());
        this.compiledPattern = this.parser.compilePattern(runtime.newString(format), true);
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
        this.cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        this.cal.setTime(utc);
    }

    public DateTimeZone getDefaultTimeZone()
    {
        return defaultTimeZone;
    }

    public Timestamp parse(String text) throws TimestampParseException
    {
        if (Strings.isNullOrEmpty(text)) {
            throw new TimestampParseException("text is null or empty string.");
        }

	RubyDateParser.FormatBag bag = parser.parseInternal(compiledPattern, text);
        if (bag == null) {
            throw new TimestampParseException("Cannot parse '" + text + "' by '" + format + "'");
        }
	bag.setYearIfNotSet(cal.get(Calendar.YEAR));
	bag.setMonthIfNotSet(cal.get(Calendar.MONTH) + 1);
	bag.setMdayIfNotSet(cal.get(Calendar.DAY_OF_MONTH));
        LocalTime local = bag.makeLocalTime();
        String zone = local.getZone();
        DateTimeZone timeZone = defaultTimeZone;
        if (zone != null) {
            // TODO cache parsed zone?
            timeZone = parseDateTimeZone(zone);
            if (timeZone == null) {
                throw new TimestampParseException("Invalid time zone name '" + text + "'");
            }
        }

        long sec = timeZone.convertLocalToUTC(local.getSeconds()*1000, false) / 1000;

        return Timestamp.ofEpochSecond(sec, local.getNsecFraction());
    }
}
