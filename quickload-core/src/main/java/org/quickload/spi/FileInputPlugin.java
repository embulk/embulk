package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.plugin.PluginManager;

public abstract class FileInputPlugin <T extends FileInputTask>
        extends BasicInputPlugin<T>
{
    public FileInputPlugin(PluginManager pluginManager)
    {
        super(pluginManager);
    }

    public abstract T getTask(ConfigSource config);

    public abstract InputProcessor startFileInputProcessor(T task,
            int processorIndex, BufferOperator op);

    public ParserPlugin getParserPlugin(String configExpresssion)
    {
        // return new CSVParserPlugin(...);
        return pluginManager.newPlugin(ParserPlugin.class, configExpresssion);
    }

    @Override
    public InputProcessor startProcessor(T task,
            int processorIndex, OutputOperator op)
    {
        BufferOperator parser = getParserPlugin(task.getConfigExpression()).openOperator(task.getParserTask(), processorIndex, op);
        return startFileInputProcessor(task, processorIndex, parser);
    }
}
