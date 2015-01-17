package org.embulk.spi;

import org.slf4j.Logger;
import org.slf4j.ILoggerFactory;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.embulk.config.Task;
import org.embulk.config.ModelManager;
import org.embulk.config.CommitReport;
import org.embulk.config.NextConfig;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.DataSourceImpl;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.PluginManager;

import java.util.TimeZone;

public class ExecSession
{
    private final Injector injector;
    private final ConfigSource execConfig;
    private final ILoggerFactory loggerFactory;
    private final ModelManager modelManager;
    private final PluginManager pluginManager;
    private final BufferAllocator bufferAllocator;

    public ExecSession(Injector injector, ConfigSource execConfig)
    {
        super();
        this.injector = injector;
        this.execConfig = execConfig;
        this.loggerFactory = injector.getInstance(ILoggerFactory.class);
        this.modelManager = injector.getInstance(ModelManager.class);
        this.pluginManager = injector.getInstance(PluginManager.class);
        this.bufferAllocator = injector.getInstance(BufferAllocator.class);
    }

    public Injector getInjector()
    {
        return injector;
    }

    public long getTransactionTime()
    {
        return execConfig.get(Long.class, "transaction_time", 0L);
    }

    public TimeZone getTransactionTimeZone()
    {
        String tzName = execConfig.get(String.class, "transaction_timezone", "UTC");
        return TimeZone.getTimeZone(tzName);
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
}
