package org.quickload.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.quickload.record.Schema;
import org.quickload.config.ConfigException;

// "exec:" config loaded by PluginExecutors.newExecTask
public class ExecConfig
{
    protected Schema schema;
    protected int processorCount;
    protected String uniqueTransactionName;

    public ExecConfig()
    {
    }

    @JsonCreator
    public void ExecConfig(
            @JsonProperty("schema") Schema schema,
            @JsonProperty("processorCount") int processorCount,
            @JsonProperty("uniqueTransactionName") String uniqueTransactionName)
    {
        this.schema = schema;
        this.processorCount = processorCount;
        this.uniqueTransactionName = uniqueTransactionName;
    }

    // for ExecTask
    void set(ExecConfig copy)
    {
        this.schema = copy.schema;
        this.processorCount = copy.processorCount;
    }

    @JsonProperty("schema")
    public Schema getSchema()
    {
        return schema;
    }

    public void setSchema(Schema schema)
    {
        this.schema = schema;
    }

    @JsonProperty("processorCount")
    public int getProcessorCount()
    {
        return processorCount;
    }

    public void setProcessorCount(int processorCount)
    {
        this.processorCount = processorCount;
    }

    @JsonProperty("uniqueTransactionName")
    public String getUniqueTransactionName()
    {
        return uniqueTransactionName;
    }

    public void setUniqueTransactionName(String uniqueTransactionName)
    {
        this.uniqueTransactionName = uniqueTransactionName;
    }

    public void validate()
    {
        if (processorCount <= 0) {
            throw new ConfigException("processorCount must be >= 1");
        }
        if (schema == null) {
            throw new ConfigException("schema must not be set");
        }
    }
}
