package org.quickload.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.quickload.record.Schema;

public class ProcTaskSource
{
    private final Schema schema;
    private final int processorCount;

    @JsonCreator
    public ProcTaskSource(
            @JsonProperty("schema") Schema schema,
            @JsonProperty("processorCount") int processorCount)
    {
        this.schema = schema;
        this.processorCount = processorCount;
    }

    @JsonProperty("schema")
    public Schema getSchema()
    {
        return schema;
    }

    @JsonProperty("processorCount")
    public int getProcessorCount()
    {
        return processorCount;
    }
}
