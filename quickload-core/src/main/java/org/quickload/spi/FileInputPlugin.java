package org.quickload.spi;

import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.plugin.PluginManager;

public abstract class FileInputPlugin
        extends BasicInputPlugin
{
    protected final PluginManager pluginManager;  // TODO get from ProcTask or ProcConfig?
    private ParserPlugin parser;

    public FileInputPlugin(PluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    public abstract TaskSource getFileInputTask(ProcConfig proc, ConfigSource config);

    public abstract InputProcessor startFileInputProcessor(ProcTask proc,
            TaskSource taskSource, int processorIndex, BufferOperator next);

    public interface InputTask
            extends Task
    {
        @Config("in:parser_type") // TODO temporarily added 'in:'
        @NotNull
        public JsonNode getParserType();

        public TaskSource getParserTask();
        public void setParserTask(TaskSource source);

        public TaskSource getFileInputTask();
        public void setFileInputTask(TaskSource task);
    }

    public ParserPlugin newParserPlugin(JsonNode typeConfig)
    {
        return pluginManager.newPlugin(ParserPlugin.class, typeConfig);
    }

    @Override
    public TaskSource getInputTask(ProcConfig proc, ConfigSource config)
    {
        InputTask task = config.loadTask(InputTask.class);
        parser = newParserPlugin(task.getParserType());
        task.setParserTask(parser.getParserTask(proc, config));
        task.setFileInputTask(getFileInputTask(proc, config));
        return config.dumpTask(task);
    }

    @Override
    public InputProcessor startInputProcessor(ProcTask proc,
            TaskSource taskSource, int processorIndex, PageOperator next)
    {
        InputTask task = taskSource.loadTask(InputTask.class);
        parser = newParserPlugin(task.getParserType());
        return startFileInputProcessor(proc, task.getFileInputTask(), processorIndex,
                parser.openBufferOperator(proc, task.getParserTask(), processorIndex, next));
    }

    @Override
    public void shutdown()
    {
        if (parser != null) {
            parser.shutdown();
        }
    }
}
