package org.quickload.spi;

import org.quickload.plugin.PluginManager;

public abstract class FileOutputPlugin <T extends FileOutputTask>
        extends BasicOutputPlugin<T>
{
    public FileOutputPlugin(PluginManager pluginManager) { super(pluginManager); }

    public abstract BufferOperator openFileOutputOperator(T task, int processorIndex);

    public FormatterPlugin getFormatterPlugin(String configExpression)
    {
        return pluginManager.newPlugin(FormatterPlugin.class, configExpression);
    }

    public OutputOperator openOperator(T task, int processorIndex)
    {
        BufferOperator op = openFileOutputOperator(task, processorIndex);
        return getFormatterPlugin(task.getConfigExpression()).openOperator(task.getFormatterTask(), processorIndex, op);
    }
}
