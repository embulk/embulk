package org.embulk.spi.time;

import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.base.Optional;
import org.jruby.Ruby;
import org.jruby.embed.ScriptingContainer;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
import org.embulk.spi.util.LineEncoder;
import org.jruby.util.RubyDateFormatter;

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

    private final RubyDateFormatter formatter;
    private final List<RubyDateFormatter.Token> compiledPattern;
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
        Ruby runtime = jruby.getProvider().getRuntime();
        this.formatter = runtime.getCurrentContext().getRubyDateFormatter();
        this.compiledPattern = this.formatter.compilePattern(runtime.newString(format), false);
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
        DateTime dt = new DateTime(value.getEpochSecond() * 1000 + value.getNano() / 1000000, timeZone);
        long nsec = value.getNano() % 1000000;
        return formatter.format(compiledPattern, dt, nsec, null).toString();
    }
}
