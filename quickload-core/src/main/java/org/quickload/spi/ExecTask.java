package org.quickload.spi;

import com.google.inject.Injector;
import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.Task;
import org.quickload.config.ModelManager;
import org.quickload.plugin.PluginManager;
import org.quickload.exec.BufferManager;
import org.quickload.buffer.BufferAllocator;
import org.quickload.record.PageAllocator;
import org.quickload.channel.BufferChannel;
import org.quickload.channel.FileBufferChannel;
import org.quickload.channel.PageChannel;

public class ExecTask
        extends ExecConfig
{
    private final Injector injector;
    private final ModelManager modelManager;
    private final PluginManager pluginManager;
    private final BufferManager bufferManager;
    private final NoticeLogger noticeLogger;

    public ExecTask(Injector injector, NoticeLogger noticeLogger)
    {
        super();
        this.injector = injector;
        this.noticeLogger = noticeLogger;
        this.modelManager = injector.getInstance(ModelManager.class);
        this.pluginManager = injector.getInstance(PluginManager.class);
        this.bufferManager = injector.getInstance(BufferManager.class);
    }

    public ExecTask(Injector injector, NoticeLogger noticeLogger, ExecConfig execConfig)
    {
        this(injector, noticeLogger);
        set(execConfig);
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

    public PageAllocator getPageAllocator()
    {
        return bufferManager;
    }

    public BufferAllocator getBufferAllocator()
    {
        return bufferManager;
    }

    public Injector getInjector()
    {
        return injector;
    }

    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig)
    {
        return pluginManager.newPlugin(iface, typeConfig);
    }

    public NoticeLogger notice()
    {
        return noticeLogger;
    }

    public PluginThread startPluginThread(Runnable runnable)
    {
        // TODO use cached thread pool
        // TODO inject thread pool manager
        // TODO set thread name
        return PluginThread.start(runnable);
    }

    public BufferChannel newBufferChannel()
    {
        return new BufferChannel(32*1024*1024);  // TODO configurable buffer size
    }

    public FileBufferChannel newFileBufferChannel()
    {
        return new FileBufferChannel(32*1024*1024);  // TODO configurable buffer size
    }

    public PageChannel newPageChannel()
    {
        return new PageChannel(32*1024*1024);  // TODO configurable buffer size
    }
}
