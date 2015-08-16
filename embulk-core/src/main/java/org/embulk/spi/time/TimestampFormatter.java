package org.embulk.spi.time;

import java.util.Locale;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.base.Optional;
import org.jruby.embed.ScriptingContainer;
import org.jruby.util.RubyDateFormat;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
import org.embulk.spi.util.LineEncoder;

public class TimestampFormatter
{
    @Deprecated
    public interface FormatterTask
            extends org.embulk.config.Task
    {
        @Config("timezone")
        @ConfigDefault("\"UTC\"")
        public DateTimeZone getTimeZone();

        @ConfigInject
        public ScriptingContainer getJRuby();
    }

    public interface Task
    {
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        public DateTimeZone getDefaultTimeZone();

        @Config("default_timestamp_format")
        @ConfigDefault("\"%Y-%m-%d %H:%M:%S.%6N %z\"")
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

    private final RubyDateFormat dateFormat;
    private final DateTimeZone timeZone;

    @Deprecated
    public TimestampFormatter(String format, FormatterTask task)
    {
        this(task.getJRuby(), format, task.getTimeZone());
    }

    public TimestampFormatter(Task task, Optional<? extends TimestampColumnOption> columnOption)
    {
        this(task.getJRuby(),
                columnOption.isPresent() ?
                    columnOption.get().getFormat().or(task.getDefaultTimestampFormat())
                    : task.getDefaultTimestampFormat(),
                columnOption.isPresent() ?
                    columnOption.get().getTimeZone().or(task.getDefaultTimeZone())
                    : task.getDefaultTimeZone());
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
