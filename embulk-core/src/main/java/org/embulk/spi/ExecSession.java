package org.embulk.spi;

//import org.slf4j.Logger;
import org.embulk.config.Task;
import org.embulk.config.ModelManager;
import org.embulk.config.CommitReport;
import org.embulk.config.NextConfig;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.PluginManager;
import com.google.inject.Injector;

public class ExecSession
{
    //private final Logger logger;

    private final Injector injector;
    private final ModelManager modelManager;
    private final PluginManager pluginManager;
    private final BufferAllocator bufferAllocator;
    //private final NoticeLogger noticeLogger;

    ExecSession(Injector injector /*, Logger logger, NoticeLogger noticeLogger*/)
    {
        super();
        this.injector = injector;
        //this.logger = logger;
        //this.noticeLogger = noticeLogger;
        this.modelManager = injector.getInstance(ModelManager.class);
        this.pluginManager = injector.getInstance(PluginManager.class);
        this.bufferAllocator = injector.getInstance(BufferAllocator.class);
    }

    //public Logger getLogger()
    //{
    //    return logger;
    //}

    //public NoticeLogger notice()
    //{
    //    return noticeLogger;
    //}

    public Injector getInjector()
    {
        return injector;
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
        return new CommitReport(modelManager);
    }

    public NextConfig newNextConfig()
    {
        return new NextConfig(modelManager);
    }

    public ConfigSource newConfigSource()
    {
        return new ConfigSource(modelManager);
    }

    public TaskSource newTaskSource()
    {
        return new TaskSource(modelManager);
    }
}
