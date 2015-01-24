package org.embulk.spi;

import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.ILoggerFactory;
import com.google.inject.Injector;
import org.embulk.config.ModelManager;
import org.embulk.config.CommitReport;
import org.embulk.config.NextConfig;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.DataSourceImpl;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.PluginManager;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampFormat;
import org.embulk.spi.time.TimestampFormatter;
import org.embulk.spi.time.TimestampFormatter.FormatterTask;

public class ExecSession
{
    private final Injector injector;
    private final ConfigSource execConfig;
    private final ILoggerFactory loggerFactory;
    private final ModelManager modelManager;
    private final PluginManager pluginManager;
    private final BufferAllocator bufferAllocator;

    private final Timestamp sessionCreatedTime;

    public ExecSession(Injector injector, ConfigSource execConfig)
    {
        super();
        this.injector = injector;
        this.execConfig = execConfig;
        this.loggerFactory = injector.getInstance(ILoggerFactory.class);
        this.modelManager = injector.getInstance(ModelManager.class);
        this.pluginManager = injector.getInstance(PluginManager.class);
        this.bufferAllocator = injector.getInstance(BufferAllocator.class);

        this.sessionCreatedTime = Timestamp.ofEpochMilli(System.currentTimeMillis());
    }

    public Injector getInjector()
    {
        return injector;
    }

    public Timestamp getTransactionTime()
    {
        return execConfig.get(Timestamp.class, "transaction_time", sessionCreatedTime);
    }

    public DateTimeZone getTransactionTimeZone()
    {
        String timezone = execConfig.get(String.class, "transaction_time_zone", "UTC");
        return TimestampFormat.parseDateTimeZone(timezone);
    }

    public Logger getLogger(String name)
    {
        return loggerFactory.getLogger(name);
    }

    public Logger getLogger(Class<?> name)
    {
        return loggerFactory.getLogger(name.getName());
    }

    public BufferAllocator getBufferAllocator()
    {
        return bufferAllocator;
    }

    public <T> T newPlugin(Class<T> iface, PluginType type)
    {
        return pluginManager.newPlugin(iface, type);
    }

    public CommitReport newCommitReport()
    {
        return new DataSourceImpl(modelManager);
    }

    public NextConfig newNextConfig()
    {
        return new DataSourceImpl(modelManager);
    }

    public ConfigSource newConfigSource()
    {
        return new DataSourceImpl(modelManager);
    }

    public TaskSource newTaskSource()
    {
        return new DataSourceImpl(modelManager);
    }

    public TimestampFormatter newTimestampFormatter(String format, DateTimeZone timezone)
    {
        ConfigSource config = Exec.newConfigSource();
        config.set("timezone", timezone.getID());
        FormatterTask formatterTask = config.loadConfig(FormatterTask.class);
        return new TimestampFormatter(format, formatterTask);
    }
}
