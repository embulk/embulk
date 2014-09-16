package org.quickload.spi;

import java.util.List;

import org.quickload.config.ConfigSource;

public abstract class FileOutputPlugin <T extends FileOutputTask>
        extends BasicOutputPlugin<T>
{
    public abstract T getOutputTask(ConfigSource config, InputTask input);

    public abstract BufferOperator openFileOutputOperator(T task, int processorIndex);

    public abstract void commit(T task, List<Report> reports);

    public abstract void abort(T task);

    public FormatterPlugin getFormatterPlugin(String type)
    {
        return null;  // TODO
    }

    public OutputOperator openOutputOperator(T task, int processorIndex)
    {
        BufferOperator op = openFileOutputOperator(task, processorIndex);
        return getFormatterPlugin(task.getFormatterType()).openOperator(task.getFormatterTask(), processorIndex, op);
    }
}
