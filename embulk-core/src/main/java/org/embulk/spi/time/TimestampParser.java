package org.embulk.spi.time;

import org.joda.time.DateTimeZone;
import com.google.common.base.Optional;
import org.jruby.embed.ScriptingContainer;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
import org.jruby.Ruby;
import org.jruby.util.RubyDateFormatter;
import org.jruby.util.RubyDateParser;
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
    }

    private final DateTimeZone defaultTimeZone;

    private final RubyDateParser parser;
    private final List<RubyDateFormatter.Token> compiledPattern;

    @Deprecated
    public TimestampParser(String format, ParserTask task)
    {
        this(task.getJRuby(), format, task.getDefaultTimeZone());
    }

    TimestampParser(Task task)
    {
        this(task.getJRuby(), task.getDefaultTimestampFormat(), task.getDefaultTimeZone());
    }

    public TimestampParser(Task task, TimestampColumnOption columnOption)
    {
        this(task.getJRuby(),
                columnOption.getFormat().or(task.getDefaultTimestampFormat()),
                columnOption.getTimeZone().or(task.getDefaultTimeZone()));
    }

    public TimestampParser(ScriptingContainer jruby, String format, DateTimeZone defaultTimeZone)
    {
        // TODO get default current time from ExecTask.getExecTimestamp
        Ruby runtime = jruby.getProvider().getRuntime();
        this.parser = new RubyDateParser(runtime.getCurrentContext());
        this.compiledPattern = this.parser.compilePattern(runtime.newString(format), true);
        this.defaultTimeZone = defaultTimeZone;
    }

    public DateTimeZone getDefaultTimeZone()
    {
        return defaultTimeZone;
    }

    public Timestamp parse(String text) throws TimestampParseException
    {
        LocalTime local = parser.parseInternal(compiledPattern, text).makeLocalTime();

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
