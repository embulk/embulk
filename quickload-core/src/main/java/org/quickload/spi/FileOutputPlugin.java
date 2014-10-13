package org.quickload.spi;

import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.plugin.PluginManager;

public abstract class FileOutputPlugin <T extends FileOutputTask>
        extends BasicOutputPlugin<T>
{
    public FileOutputPlugin(PluginManager pluginManager) { super(pluginManager); }

    public abstract BufferOperator openFileOutputOperator(T task, int processorIndex);

    public FormatterPlugin getFormatterPlugin(JsonNode typeConfig)
    {
        return pluginManager.newPlugin(FormatterPlugin.class, typeConfig);
    }

    public OutputOperator openOperator(T task, int processorIndex)
    {
        return null;
        // TODO
        //BufferOperator op = openFileOutputOperator(task, processorIndex);
        //return getFormatterPlugin(task.getConfigExpression()).openOperator(task.getFormatterTask(), processorIndex, op);
    }
}
