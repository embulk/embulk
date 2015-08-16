package org.embulk.spi.time;

import org.joda.time.DateTimeZone;
import com.google.common.base.Optional;
import org.jruby.embed.ScriptingContainer;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
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

    private final JRubyTimeParserHelper helper;
    private final DateTimeZone defaultTimeZone;

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
        JRubyTimeParserHelperFactory helperFactory = (JRubyTimeParserHelperFactory) jruby.runScriptlet("Embulk::Java::TimeParserHelper::Factory.new");
        // TODO get default current time from ExecTask.getExecTimestamp
        this.helper = (JRubyTimeParserHelper) helperFactory.newInstance(format, 1970, 1, 1, 0, 0, 0, 0);  // TODO default time zone
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
