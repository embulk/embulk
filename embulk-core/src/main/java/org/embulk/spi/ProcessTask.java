package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.plugin.PluginType;
import org.embulk.spi.util.ExecutorsInternal;

public class ProcessTask {
    private final PluginType inputPluginType;
    private final PluginType outputPluginType;
    private final List<PluginType> filterPluginTypes;
    private final TaskSource inputTaskSource;
    private final TaskSource outputTaskSource;
    private final List<TaskSource> filterTaskSources;
    private final List<Schema> schemas;
    private final Schema executorSchema;
    private TaskSource executorTaskSource;

    public ProcessTask(
            final PluginType inputPluginType,
            final PluginType outputPluginType,
            final List<PluginType> filterPluginTypes,
            final TaskSource inputTaskSource,
            final TaskSource outputTaskSource,
            final List<TaskSource> filterTaskSources,
            final List<Schema> schemas,
            final Schema executorSchema,
            final TaskSource executorTaskSource) {
        this.inputPluginType = inputPluginType;
        this.outputPluginType = outputPluginType;
        this.filterPluginTypes = filterPluginTypes;
        this.inputTaskSource = inputTaskSource;
        this.outputTaskSource = outputTaskSource;
        this.filterTaskSources = filterTaskSources;
        this.schemas = schemas;
        this.executorSchema = executorSchema;
        this.executorTaskSource = executorTaskSource;
    }

    public PluginType getInputPluginType() {
        return inputPluginType;
    }

    public PluginType getOutputPluginType() {
        return outputPluginType;
    }

    public List<PluginType> getFilterPluginTypes() {
        return filterPluginTypes;
    }

    public TaskSource getInputTaskSource() {
        return inputTaskSource;
    }

    public TaskSource getOutputTaskSource() {
        return outputTaskSource;
    }

    public List<TaskSource> getFilterTaskSources() {
        return filterTaskSources;
    }

    public List<Schema> getFilterSchemas() {
        return schemas;
    }

    public Schema getExecutorSchema() {
        return executorSchema;
    }

    public Schema getInputSchema() {
        return ExecutorsInternal.getInputSchema(schemas);
    }

    public Schema getOutputSchema() {
        return ExecutorsInternal.getOutputSchema(schemas);
    }

    public void setExecutorTaskSource(TaskSource executorTaskSource) {
        this.executorTaskSource = executorTaskSource;
    }

    public TaskSource getExecutorTaskSource() {
        return executorTaskSource;
    }
}
