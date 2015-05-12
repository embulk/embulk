package org.embulk.spi.time;

import java.util.Locale;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jruby.embed.ScriptingContainer;
import org.jruby.util.RubyDateFormat;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.embulk.spi.util.LineEncoder;

public class TimestampFormatter
{
    public interface FormatterTask
            extends Task
    {
        @Config("timezone")
        @ConfigDefault("\"UTC\"")
        public DateTimeZone getTimeZone();

        @ConfigInject
        public ScriptingContainer getJRuby();
    }

    private final RubyDateFormat dateFormat;
    private final DateTimeZone timeZone;

    public TimestampFormatter(String format, FormatterTask task)
    {
        this(task.getJRuby(), format, task.getTimeZone());
    }

    public TimestampFormatter(ScriptingContainer jruby, String format, DateTimeZone timeZone)
    {
        this.timeZone = timeZone;
        this.dateFormat = new RubyDateFormat(format, Locale.ENGLISH, true);
    }

    public DateTimeZone getTimeZone()
    {
        return timeZone;
    }

    public void format(Timestamp value, LineEncoder encoder)
    {
        // TODO optimize by directly appending to internal buffer
        encoder.addText(format(value));
    }

    public String format(Timestamp value)
    {
        // TODO optimize by using reused StringBuilder
        dateFormat.setDateTime(new DateTime(value.getEpochSecond()*1000, timeZone));
        dateFormat.setNSec(value.getNano());
        return dateFormat.format(null);
    }
}
