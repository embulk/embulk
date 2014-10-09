package org.quickload.spi;

import java.util.List;

import org.quickload.config.ConfigSource;
import org.quickload.plugin.PluginManager;

public abstract class BasicInputPlugin <T extends InputTask>
        implements InputPlugin, InputTransaction
{
    protected PluginManager pluginManager;

    private ConfigSource config;
    private T task;

    public BasicInputPlugin(PluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    public abstract T getTask(ConfigSource config);

    public abstract InputProcessor startProcessor(T task,
            int processorIndex, OutputOperator op);

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
     * InputTransaction
     */
    @Override
    public T getInputTask()
    {
        task = getTask(config);
        return task;
    }

    /*
     * InputTransaction
     */
    @Override
    public void begin()
    {
        begin(task);
    }

    /*
     * InputTransaction
     */
    @Override
    public void commit(List<Report> reports)
    {
        commit(task, reports);
    }

    /*
     * InputTransaction
     */
    @Override
    public void abort()
    {
        abort(task);
    }

    /*
     * InputPlugin
     */
    @Override
    public InputTransaction newInputTransaction(ConfigSource config)
    {
        this.config = config;
        return this;
    }

    /*
     * InputPlugin
     */
    public InputProcessor startInputProcessor(InputTask task,
            int processorIndex, OutputOperator op)
    {
        return startProcessor((T) task, processorIndex, op);
    }

    /*
     * InputPlugin
     */
    @Override
    public void shutdown()
    {
    }
}
