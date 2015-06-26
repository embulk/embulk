package org.embulk.spi;

import java.util.List;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.embulk.plugin.PluginType;
import org.embulk.config.TaskSource;
import org.embulk.spi.Schema;
import org.embulk.spi.util.Executors;
import org.embulk.spi.type.TimestampType;

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

    public ProcessTask(
            PluginType inputPluginType,
            PluginType outputPluginType,
            List<PluginType> filterPluginTypes,
            TaskSource inputTaskSource,
            TaskSource outputTaskSource,
            List<TaskSource> filterTaskSources,
            List<Schema> schemas,
            Schema executorSchema,
            TaskSource executorTaskSource)
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

    // TODO Because TimestampType doesn't store timestamp_format, serializing and deserializing
    // Schema loses timestamp_format information. Here uses SchemaConfig instead to preseve it.

    @JsonCreator
    ProcessTask(
            @JsonProperty("inputType") PluginType inputPluginType,
            @JsonProperty("outputType") PluginType outputPluginType,
            @JsonProperty("filterTypes") List<PluginType> filterPluginTypes,
            @JsonProperty("inputTask") TaskSource inputTaskSource,
            @JsonProperty("outputTask") TaskSource outputTaskSource,
            @JsonProperty("filterTasks") List<TaskSource> filterTaskSources,
            @JsonProperty("schemas") List<SchemaConfig> schemas,
            @JsonProperty("executorSchema") SchemaConfig executorSchema,
            @JsonProperty("executorTask") TaskSource executorTaskSource)
    {
        this(inputPluginType, outputPluginType, filterPluginTypes,
                inputTaskSource, outputTaskSource, filterTaskSources,
                ImmutableList.copyOf(Lists.transform(schemas,
                        new Function<SchemaConfig, Schema>()
                        {
                            public Schema apply(SchemaConfig s)
                            {
                                return s.toSchema();
                            }
                        }
                    )),
                executorSchema.toSchema(),
                executorTaskSource);
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

    @JsonIgnore
    public List<Schema> getFilterSchemas()
    {
        return schemas;
    }

    @JsonProperty("schemas")
    public List<SchemaConfig> getFilterSchemaConfigs()
    {
        return Lists.transform(schemas,
                new Function<Schema, SchemaConfig>()
                {
                    public SchemaConfig apply(Schema schema)
                    {
                        return schemaToSchemaConfig(schema);
                    }
                });
    }

    @JsonIgnore
    public Schema getExecutorSchema()
    {
        return executorSchema;
    }

    @JsonProperty("executorSchema")
    SchemaConfig getExecutorSchemaConfig()
    {
        return schemaToSchemaConfig(executorSchema);
    }

    private static SchemaConfig schemaToSchemaConfig(Schema s)
    {
        return new SchemaConfig(Lists.transform(s.getColumns(),
                    new Function<Column, ColumnConfig>()
                    {
                        public ColumnConfig apply(Column c)
                        {
                            return new ColumnConfig(c.getConfigSource());
                        }
                    }
                ));
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
