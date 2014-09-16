package org.quickload.spi;

import java.util.List;

import org.quickload.config.ConfigSource;

public abstract class BasicOutputPlugin <T extends OutputTask>
        implements OutputPlugin, OutputTransaction
{
    private ConfigSource config;
    private T task;

    public abstract T getTask(ConfigSource config, InputTask input);

    public abstract OutputOperator openOperator(T task, int processorIndex);

    public abstract void begin(T task);

    public abstract void commit(T task, List<Report> reports);

    public abstract void abort(T task);

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

    @Override
    public OutputOperator openOutputOperator(OutputTask task, int processorIndex)
    {
        return openOperator((T) task, processorIndex);
    }

    /*
     * OutputPlugin
     */
    @Override
    public void shutdown()
    {
    }
}
