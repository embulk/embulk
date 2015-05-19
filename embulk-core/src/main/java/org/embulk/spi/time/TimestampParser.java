package org.embulk.spi.time;

import org.joda.time.DateTimeZone;
import org.jruby.embed.ScriptingContainer;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
import org.jruby.Ruby;
import org.jruby.util.RubyDateFormatter;
import org.jruby.util.RubyDateParser;
import org.jruby.util.RubyDateParser.Temporal;

import java.util.List;

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

    private final DateTimeZone defaultTimeZone;

    private final RubyDateParser parser;
    private final List<RubyDateFormatter.Token> compiledPattern;

    public TimestampParser(String format, ParserTask task)
    {
        this(task.getJRuby(), format, task.getDefaultTimeZone());
    }

    // TODO this is still private because this might need current time
    private TimestampParser(ScriptingContainer jruby, String format, DateTimeZone defaultTimeZone)
    {
        // TODO get default current time from ExecTask.getExecTimestamp
        Ruby runtime = jruby.getProvider().getRuntime();
        this.parser = new RubyDateParser(runtime.getCurrentContext());
        this.compiledPattern = this.parser.compilePattern(runtime.newString(format), true);
        this.defaultTimeZone = defaultTimeZone;
    }

    public Timestamp parse(String text) throws TimestampParseException
    {
        Temporal tmp = parser.parse(compiledPattern, text);
        String zone = tmp.getZone();
        DateTimeZone timeZone = defaultTimeZone;
        if (zone != null) {
            // TODO cache parsed zone?
            timeZone = parseDateTimeZone(zone);
            if (timeZone == null) {
                throw new TimestampParseException();
            }
        }

        long localSec = tmp.getSec();
        long sec = timeZone.convertLocalToUTC(localSec*1000, false) / 1000;

        return Timestamp.ofEpochSecond(sec, tmp.getNsec()); // maybe millisec/usec convert
    }
}
