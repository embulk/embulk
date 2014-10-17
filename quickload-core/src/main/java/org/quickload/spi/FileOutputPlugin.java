package org.quickload.spi;

import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.plugin.PluginManager;

public abstract class FileOutputPlugin
        extends BasicOutputPlugin
{
    protected final PluginManager pluginManager;  // TODO get from ProcTask?
    private FormatterPlugin formatter;

    public FileOutputPlugin(PluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    public abstract TaskSource getFileOutputTask(ProcTask proc, ConfigSource config);

    public abstract BufferOperator openBufferOutputOperator(ProcTask proc,
            TaskSource taskSource, int processorIndex);

    public interface OutputTask
            extends Task
    {
        @Config("out:formatter_type")
        @NotNull
        public JsonNode getFormatterType();

        public TaskSource getFormatterTask();
        public void setFormatterTask(TaskSource task);

        public TaskSource getFileOutputTask();
        public void setFileOutputTask(TaskSource task);
    }

    public FormatterPlugin newFormatterPlugin(JsonNode typeConfig)
    {
        return pluginManager.newPlugin(FormatterPlugin.class, typeConfig);
    }

    @Override
    public TaskSource getOutputTask(ProcTask proc, ConfigSource config)
    {
        OutputTask task = config.loadTask(OutputTask.class);
        formatter = newFormatterPlugin(task.getFormatterType());
        task.setFormatterTask(formatter.getFormatterTask(proc, config));
        task.setFileOutputTask(getFileOutputTask(proc, config));
        return config.dumpTask(task);
    }

    @Override
    public PageOperator openPageOperator(ProcTask proc,
            TaskSource taskSource, int processorIndex)
    {
        OutputTask task = taskSource.loadTask(OutputTask.class);
        formatter = newFormatterPlugin(task.getFormatterType());
        return formatter.openPageOperator(proc, task.getFormatterTask(), processorIndex,
                openBufferOutputOperator(proc, task.getFileOutputTask(), processorIndex));
    }

    @Override
    public void shutdown()
    {
        if (formatter != null) {
            formatter.shutdown();
        }
    }
}
