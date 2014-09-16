package org.quickload.spi;

import org.quickload.config.ConfigSource;

public abstract class FileInputPlugin <T extends FileInputTask>
        extends BasicInputPlugin<T>
{
    public abstract T getTask(ConfigSource config);

    public abstract InputProcessor startFileInputProcessor(T task,
            int processorIndex, BufferOperator op);

    public ParserPlugin getParserPlugin(String type)
    {
        return null;  // TODO
    }

    @Override
    public InputProcessor startProcessor(T task,
            int processorIndex, OutputOperator op)
    {
        BufferOperator parser = getParserPlugin(task.getParserType()).openOperator(task.getParserTask(), processorIndex, op);
        return startFileInputProcessor(task, processorIndex, parser);
    }
}
