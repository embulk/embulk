package org.quickload.spi;

import java.util.List;
import org.quickload.config.ConfigSource;
import org.quickload.plugin.PluginManager;
import org.quickload.config.TaskSource;

public abstract class BasicOutputPlugin <T extends OutputTask>
        implements OutputPlugin, OutputTransaction
{
    protected PluginManager pluginManager;

    private ConfigSource config;
    private T task;

    public BasicOutputPlugin(PluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    public abstract T getTask(ConfigSource config, InputTask input);

    public abstract OutputOperator openOperator(T task, int processorIndex);

    public void begin(T task)
    {
    }

    public void commit(T task, List<Report> reports)
    {
    }

    public void abort(T task)
    {
    }

    /*
     * OutputTransaction
     */
    @Override
    public T getOutputTask(InputTask input)
    {
        task = getTask(config, input);
        return task;
    }

    /*
     * OutputTransaction
     */
    @Override
    public void begin()
    {
        begin(task);
    }

    /*
     * OutputTransaction
     */
    @Override
    public void commit(List<Report> reports)
    {
        commit(task, reports);
    }

    /*
     * OutputTransaction
     */
    @Override
    public void abort()
    {
        abort(task);
    }

    /*
     * OutputPlugin
     */
    @Override
    public OutputTransaction newOutputTransaction(ConfigSource config)
    {
        this.config = config;
        return this;
    }

    protected Class<T> getTaskType()
    {
        return (Class<T>) BasicPluginUtils.getTaskType(getClass(), "getTask", ConfigSource.class, InputTask.class);
    }

    @Override
    public OutputOperator openOutputOperator(TaskSource taskSource, int processorIndex)
    {
        return openOperator(taskSource.load(getTaskType()), processorIndex);
    }

    /*
     * OutputPlugin
     */
    @Override
    public void shutdown()
    {
    }
}
