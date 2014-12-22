package org.embulk.spi;

//import org.slf4j.Logger;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.ModelManager;
import org.embulk.plugin.PluginManager;
import com.fasterxml.jackson.databind.JsonNode;
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

    public <T extends Task> T loadConfig(ConfigSource config, Class<T> taskType)
    {
        return modelManager.readTaskConfig(config, taskType);
    }

    public <T extends Task> T loadTask(TaskSource taskSource, Class<T> taskType)
    {
        return modelManager.readObject(taskSource, taskType);
    }

    public TaskSource dumpTask(Task task)
    {
        return modelManager.writeAsTaskSource(task);
    }

    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig)
    {
        return pluginManager.newPlugin(iface, typeConfig);
    }
}
