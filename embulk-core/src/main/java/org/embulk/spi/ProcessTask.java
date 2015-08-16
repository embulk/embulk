package org.embulk.spi;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.embulk.plugin.PluginType;
import org.embulk.config.TaskSource;
import org.embulk.spi.util.Executors;

public class ProcessTask
{
    private final PluginType inputPluginType;
    private final PluginType outputPluginType;
    private final List<PluginType> filterPluginTypes;
    private final TaskSource inputTaskSource;
    private final TaskSource outputTaskSource;
    private final List<TaskSource> filterTaskSources;
    private final List<Schema> schemas;
    private final Schema executorSchema;
    private TaskSource executorTaskSource;

    @JsonCreator
    public ProcessTask(
            @JsonProperty("inputType") PluginType inputPluginType,
            @JsonProperty("outputType") PluginType outputPluginType,
            @JsonProperty("filterTypes") List<PluginType> filterPluginTypes,
            @JsonProperty("inputTask") TaskSource inputTaskSource,
            @JsonProperty("outputTask") TaskSource outputTaskSource,
            @JsonProperty("filterTasks") List<TaskSource> filterTaskSources,
            @JsonProperty("schemas") List<Schema> schemas,
            @JsonProperty("executorSchema") Schema executorSchema,
            @JsonProperty("executorTask") TaskSource executorTaskSource)
    {
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

    @JsonProperty("inputType")
    public PluginType getInputPluginType()
    {
        return inputPluginType;
    }

    @JsonProperty("outputType")
    public PluginType getOutputPluginType()
    {
        return outputPluginType;
    }

    @JsonProperty("filterTypes")
    public List<PluginType> getFilterPluginTypes()
    {
        return filterPluginTypes;
    }

    @JsonProperty("inputTask")
    public TaskSource getInputTaskSource()
    {
        return inputTaskSource;
    }

    @JsonProperty("outputTask")
    public TaskSource getOutputTaskSource()
    {
        return outputTaskSource;
    }

    @JsonProperty("filterTasks")
    public List<TaskSource> getFilterTaskSources()
    {
        return filterTaskSources;
    }

    @JsonProperty("schemas")
    public List<Schema> getFilterSchemas()
    {
        return schemas;
    }

    @JsonProperty("executorSchema")
    public Schema getExecutorSchema()
    {
        return executorSchema;
    }

    @JsonIgnore
    public Schema getInputSchema()
    {
        return Executors.getInputSchema(schemas);
    }

    @JsonIgnore
    public Schema getOutputSchema()
    {
        return Executors.getOutputSchema(schemas);
    }

    @JsonIgnore
    public void setExecutorTaskSource(TaskSource executorTaskSource)
    {
        this.executorTaskSource = executorTaskSource;
    }

    @JsonProperty("executorTask")
    public TaskSource getExecutorTaskSource()
    {
        return executorTaskSource;
    }
}
