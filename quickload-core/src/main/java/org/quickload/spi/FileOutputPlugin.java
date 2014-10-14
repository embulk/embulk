package org.quickload.spi;

import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.ConfigSource;
import org.quickload.plugin.PluginManager;

public abstract class FileOutputPlugin <T extends FileOutputTask>
        extends BasicOutputPlugin<T>
{
    private FormatterPlugin formatter;

    public FileOutputPlugin(PluginManager pluginManager)
    {
        super(pluginManager);
    }

    public abstract T getFileOutputTask(ConfigSource config, InputTask input,
            FormatterTask formatterTask);

    public abstract BufferOperator openFileOutputOperator(T task, int processorIndex);

    public FormatterPlugin newFormatterPlugin(JsonNode typeConfig)
    {
        return pluginManager.newPlugin(FormatterPlugin.class, typeConfig);
    }

    @Override
    protected Class<T> getTaskType()
    {
        return (Class<T>) BasicPluginUtils.getTaskType(getClass(), "getFileOutputTask", ConfigSource.class, InputTask.class, FormatterTask.class);
    }

    @Override
    public T getTask(ConfigSource config, InputTask input)
    {
        FileOutputTask task = config.load(FileOutputTask.class);
        formatter = newFormatterPlugin(task.getFormatterType());
        return getFileOutputTask(config, input,
                formatter.getFormatterTask(config, input));
    }

    public OutputOperator openOperator(T task, int processorIndex)
    {
        formatter = newFormatterPlugin(task.getFormatterType());
        return formatter.openFormatterOperator(task.getFormatterTask(), processorIndex,
                openFileOutputOperator(task, processorIndex));
    }

    @Override
    public void shutdown()
    {
        if (formatter != null) {
            formatter.shutdown();
        }
    }
}
