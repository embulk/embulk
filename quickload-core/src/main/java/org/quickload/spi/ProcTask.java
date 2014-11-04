package org.quickload.spi;

import com.google.inject.Injector;
import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.ConfigException;
import org.quickload.config.Task;
import org.quickload.config.ModelManager;
import org.quickload.plugin.PluginManager;
import org.quickload.exec.BufferManager;
import org.quickload.buffer.BufferAllocator;
import org.quickload.record.Schema;
import org.quickload.record.PageAllocator;
import org.quickload.channel.PageChannel;
import org.quickload.channel.BufferChannel;

public class ProcTask
{
    private final Injector injector;
    private final ModelManager modelManager;
    private final PluginManager pluginManager;
    private final BufferManager bufferManager;

    private Schema schema;
    private int processorCount;

    public ProcTask(
            Injector injector)
    {
        this.injector = injector;
        this.modelManager = injector.getInstance(ModelManager.class);
        this.pluginManager = injector.getInstance(PluginManager.class);
        this.bufferManager = injector.getInstance(BufferManager.class);
    }

    private ProcTask(
            Injector injector,
            ProcTaskSource taskSource)
    {
        this(injector);
        this.schema = taskSource.getSchema();
        this.processorCount = taskSource.getProcessorCount();
    }

    public Schema getSchema()
    {
        return schema;
    }

    public void setSchema(Schema schema)
    {
        this.schema = schema;
    }

    public int getProcessorCount()
    {
        return processorCount;
    }

    public void setProcessorCount(int processorCount)
    {
        this.processorCount = processorCount;
    }

    public static ProcTask load(Injector injector, ProcTaskSource taskSource)
    {
        return new ProcTask(injector, taskSource);
    }

    public <T extends Task> T loadConfig(ConfigSource config, Class<T> iface)
    {
        return config.loadModel(modelManager, iface);
    }

    public <T extends Task> T loadTask(TaskSource taskSource, Class<T> iface)
    {
        return taskSource.loadModel(modelManager, iface);
    }

    public TaskSource dumpTask(Task task)
    {
        task.validate();
        return modelManager.readJsonObject(
                modelManager.writeJsonObjectNode(task),
                TaskSource.class);
    }

    public PageAllocator getPageAllocator()
    {
        return bufferManager;
    }

    public BufferAllocator getBufferAllocator()
    {
        return bufferManager;
    }

    public void validate()
    {
        if (processorCount <= 0) {
            throw new ConfigException("processorCount must be >= 1");
        }
        if (schema == null) {
            throw new ConfigException("schema must not be set");
        }
    }

    public Injector getInjector()
    {
        return injector;
    }

    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig)
    {
        return pluginManager.newPlugin(iface, typeConfig);
    }

    public void startPluginThread(PluginThread runner)
    {
        // TODO use cached thread pool
        // TODO inject thread pool manager
        // TODO set thread name
        new Thread(runner).start();
    }

    public BufferChannel newBufferChannel()
    {
        return new BufferChannel(32*1024*1024);  // TODO configurable buffer size
    }

    public PageChannel newPageChannel()
    {
        return new PageChannel(32*1024*1024);  // TODO configurable buffer size
    }

    public ProcTaskSource dump()
    {
        validate();
        return new ProcTaskSource(schema, processorCount);
    }
}
