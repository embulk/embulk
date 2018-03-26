package org.embulk.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

import org.embulk.config.TaskSource;
import org.embulk.plugin.PluginType;
import org.embulk.spi.util.Executors;

public class ProcessTask {
    private final PluginType inputPluginType;
    private final PluginType outputPluginType;
    private final List<PluginType> filterPluginTypes;
    private final Map<Reporter.Channel, PluginType> reporterPluginTypes;
    private final TaskSource inputTaskSource;
    private final TaskSource outputTaskSource;
    private final List<TaskSource> filterTaskSources;
    private final Map<Reporter.Channel, TaskSource> reporterTaskSources;
    private final List<Schema> schemas;
    private final Schema executorSchema;
    private TaskSource executorTaskSource;

    @JsonCreator
    public ProcessTask(
            @JsonProperty("inputType") PluginType inputPluginType,
            @JsonProperty("outputType") PluginType outputPluginType,
            @JsonProperty("filterTypes") List<PluginType> filterPluginTypes,
            @JsonProperty("reporterTypes") Map<Reporter.Channel, PluginType> reporterPluginTypes,
            @JsonProperty("inputTask") TaskSource inputTaskSource,
            @JsonProperty("outputTask") TaskSource outputTaskSource,
            @JsonProperty("filterTasks") List<TaskSource> filterTaskSources,
            @JsonProperty("reporterTasks") Map<Reporter.Channel, TaskSource> reporterTaskSources,
            @JsonProperty("schemas") List<Schema> schemas,
            @JsonProperty("executorSchema") Schema executorSchema,
            @JsonProperty("executorTask") TaskSource executorTaskSource) {
        this.inputPluginType = inputPluginType;
        this.outputPluginType = outputPluginType;
        this.filterPluginTypes = filterPluginTypes;
        this.reporterPluginTypes = reporterPluginTypes;
        this.inputTaskSource = inputTaskSource;
        this.outputTaskSource = outputTaskSource;
        this.filterTaskSources = filterTaskSources;
        this.reporterTaskSources = reporterTaskSources;
        this.schemas = schemas;
        this.executorSchema = executorSchema;
        this.executorTaskSource = executorTaskSource;
    }

    @JsonProperty("inputType")
    public PluginType getInputPluginType() {
        return inputPluginType;
    }

    @JsonProperty("outputType")
    public PluginType getOutputPluginType() {
        return outputPluginType;
    }

    @JsonProperty("filterTypes")
    public List<PluginType> getFilterPluginTypes() {
        return filterPluginTypes;
    }

    @JsonProperty("reporterTypes")
    public Map<Reporter.Channel, PluginType> reporterPluginTypes() {
        return reporterPluginTypes;
    }

    @JsonProperty("inputTask")
    public TaskSource getInputTaskSource() {
        return inputTaskSource;
    }

    @JsonProperty("outputTask")
    public TaskSource getOutputTaskSource() {
        return outputTaskSource;
    }

    @JsonProperty("filterTasks")
    public List<TaskSource> getFilterTaskSources() {
        return filterTaskSources;
    }

    @JsonProperty("reporterTasks")
    public Map<Reporter.Channel, TaskSource> getReporterTaskSources() {
        return reporterTaskSources;
    }

    @JsonProperty("schemas")
    public List<Schema> getFilterSchemas() {
        return schemas;
    }

    @JsonProperty("executorSchema")
    public Schema getExecutorSchema() {
        return executorSchema;
    }

    @JsonIgnore
    public Schema getInputSchema() {
        return Executors.getInputSchema(schemas);
    }

    @JsonIgnore
    public Schema getOutputSchema() {
        return Executors.getOutputSchema(schemas);
    }

    @JsonIgnore
    public void setExecutorTaskSource(TaskSource executorTaskSource) {
        this.executorTaskSource = executorTaskSource;
    }

    @JsonProperty("executorTask")
    public TaskSource getExecutorTaskSource() {
        return executorTaskSource;
    }
}
