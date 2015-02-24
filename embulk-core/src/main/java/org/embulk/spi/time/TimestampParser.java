package org.embulk.spi.time;

import org.joda.time.DateTimeZone;
import org.jruby.embed.ScriptingContainer;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import static org.embulk.spi.time.TimestampFormat.parseDateTimeZone;

public class TimestampParser
{
    public interface ParserTask
            extends Task
    {
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        public DateTimeZone getDefaultTimeZone();

        @ConfigInject
        public ScriptingContainer getJRuby();
    }

    private final JRubyTimeParserHelper helper;
    private final DateTimeZone defaultTimeZone;

    public TimestampParser(String format, ParserTask task)
    {
        this(task.getJRuby(), format, task.getDefaultTimeZone());
    }

    // TODO this is still private because this might need current time
    private TimestampParser(ScriptingContainer jruby, String format, DateTimeZone defaultTimeZone)
    {
        JRubyTimeParserHelperFactory helperFactory = (JRubyTimeParserHelperFactory) jruby.runScriptlet("Embulk::Java::TimeParserHelper::Factory.new");
        // TODO get default current time from ExecTask.getExecTimestamp
        this.helper = (JRubyTimeParserHelper) helperFactory.newInstance(format, 1970, 1, 1, 0, 0, 0, 0);  // TODO default time zone
        this.defaultTimeZone = defaultTimeZone;
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
                throw new TimestampParseException();
            }
        }

        long localSec = localUsec / 1000000;
        long usec = localUsec % 1000000;
        long sec = timeZone.convertLocalToUTC(localSec*1000, false) / 1000;

        return Timestamp.ofEpochSecond(sec, usec * 1000);
    }
}
