package org.quickload.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.quickload.record.Schema;
import org.quickload.config.ConfigException;

public class ProcConfig
{
    protected Schema schema;
    protected int processorCount;

    public ProcConfig()
    {
    }

    @JsonCreator
    public void ProcConfig(
            @JsonProperty("schema") Schema schema,
            @JsonProperty("processorCount") int processorCount)
    {
        this.schema = schema;
        this.processorCount = processorCount;
    }

    // for ProcTask
    void set(ProcConfig copy)
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
