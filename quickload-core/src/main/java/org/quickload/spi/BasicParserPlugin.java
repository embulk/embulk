package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;

public abstract class BasicParserPlugin <T extends ParserTask>
        implements ParserPlugin
{
    public abstract T getTask(ConfigSource config);

    public abstract BufferOperator openOperator(T task, int processorIndex, OutputOperator op);

    @Override
    public ParserTask getParserTask(ConfigSource config)
    {
        return getTask(config);
    }

    protected Class<T> getTaskType()
    {
        return (Class<T>) BasicPluginUtils.getTaskType(getClass(), "getTask", ConfigSource.class);
    }

    @Override
    public BufferOperator openParserOperator(TaskSource taskSource, int processorIndex, OutputOperator op)
    {
        return openOperator(taskSource.load(getTaskType()),
                    processorIndex, op);
    }
}
