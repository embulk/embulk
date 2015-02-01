package org.embulk.spi;

import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.ILoggerFactory;
import com.google.common.base.Optional;
import com.google.inject.Injector;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ModelManager;
import org.embulk.config.CommitReport;
import org.embulk.config.NextConfig;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.DataSourceImpl;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.PluginManager;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampFormatter;
import org.embulk.spi.time.TimestampFormatter.FormatterTask;

public class ExecSession
{
    private final Injector injector;
    private final ILoggerFactory loggerFactory;
    private final ModelManager modelManager;
    private final PluginManager pluginManager;
    private final BufferAllocator bufferAllocator;
    private final Timestamp transactionTime;
    private final DateTimeZone transactionTimeZone;

    public interface SessionTask
            extends Task
    {
        @Config("transaction_time")
        @ConfigDefault("null")
        Optional<Timestamp> getTransactionTime();

        @Config("transaction_time_zone")
        @ConfigDefault("\"UTC\"")
        DateTimeZone getTransactionTimeZone();
    }

    public ExecSession(Injector injector, ConfigSource execConfig)
    {
        this(injector, execConfig.loadConfig(SessionTask.class));
    }

    public ExecSession(Injector injector, TaskSource taskSource)
    {
        this(injector, taskSource.loadTask(SessionTask.class));
    }

    public ExecSession(Injector injector, SessionTask task)
    {
        this.injector = injector;
        this.loggerFactory = injector.getInstance(ILoggerFactory.class);
        this.modelManager = injector.getInstance(ModelManager.class);
        this.pluginManager = injector.getInstance(PluginManager.class);
        this.bufferAllocator = injector.getInstance(BufferAllocator.class);

        this.transactionTime = task.getTransactionTime().or(Timestamp.ofEpochMilli(System.currentTimeMillis()));  // TODO get nanoseconds for default
        this.transactionTimeZone = task.getTransactionTimeZone();
    }

    public TaskSource getSessionTaskSource()
    {
        return newTaskSource()
            .set("transaction_time", transactionTime)
            .set("transaction_time_zone", transactionTimeZone);
    }

    public Injector getInjector()
    {
        return injector;
    }

    public Timestamp getTransactionTime()
    {
        return transactionTime;
    }

    public DateTimeZone getTransactionTimeZone()
    {
        return transactionTimeZone;
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
