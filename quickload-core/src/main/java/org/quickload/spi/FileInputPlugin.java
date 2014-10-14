package org.quickload.spi;

import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.ConfigSource;
import org.quickload.plugin.PluginManager;

public abstract class FileInputPlugin <T extends FileInputTask>
        extends BasicInputPlugin<T>
{
    private ParserPlugin parser;

    public FileInputPlugin(PluginManager pluginManager)
    {
        super(pluginManager);
    }

    public abstract T getFileInputTask(ConfigSource config, ParserTask parserTask);

    public abstract InputProcessor startFileInputProcessor(T task,
            int processorIndex, BufferOperator op);

    public ParserPlugin newParserPlugin(JsonNode typeConfig)
    {
        return pluginManager.newPlugin(ParserPlugin.class, typeConfig);
    }

    @Override
    protected Class<T> getTaskType()
    {
        return (Class<T>) BasicPluginUtils.getTaskType(getClass(), "getFileInputTask", ConfigSource.class, ParserTask.class);
    }

    @Override
    public T getTask(ConfigSource config)
    {
        FileInputTask task = config.load(FileInputTask.class);
        parser = newParserPlugin(task.getParserType());
        return getFileInputTask(config,
                parser.getParserTask(config));
    }

    @Override
    public InputProcessor startProcessor(T task,
            int processorIndex, OutputOperator op)
    {
        parser = newParserPlugin(task.getParserType());
        return startFileInputProcessor(task, processorIndex,
                parser.openParserOperator(task.getParserTask(), processorIndex, op));
    }

    @Override
    public void shutdown()
    {
        if (parser != null) {
            parser.shutdown();
        }
    }
}
