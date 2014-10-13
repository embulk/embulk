package org.quickload.spi;

import com.fasterxml.jackson.databind.JsonNode;
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

    public ParserPlugin getParserPlugin(JsonNode typeConfig)
    {
        return pluginManager.newPlugin(ParserPlugin.class, typeConfig);
    }

    @Override
    public InputProcessor startProcessor(T task,
            int processorIndex, OutputOperator op)
    {
        return null;
        // TODO
        //BufferOperator parser = getParserPlugin(task.getConfigExpression()).openOperator(task.getParserTask(), processorIndex, op);
        // return startFileInputProcessor(task, processorIndex, parser);
    }
}
