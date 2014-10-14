package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;

public abstract class BasicFormatterPlugin <T extends FormatterTask>
        implements FormatterPlugin
{
    public abstract T getTask(ConfigSource config, InputTask input);

    public abstract OutputOperator openOperator(T task, int processorIndex, BufferOperator op);

    @Override
    public FormatterTask getFormatterTask(ConfigSource config, InputTask input)
    {
        return getTask(config, input);
    }

    protected Class<T> getTaskType()
    {
        return (Class<T>) BasicPluginUtils.getTaskType(getClass(), "getTask", ConfigSource.class, InputTask.class);
    }

    @Override
    public OutputOperator openFormatterOperator(TaskSource taskSource, int processorIndex, BufferOperator op)
    {
        return openOperator(taskSource.load(getTaskType()),
                    processorIndex, op);
    }
}
