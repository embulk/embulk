package org.quickload.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.quickload.record.Schema;
import org.quickload.config.ConfigException;

public class ProcConfig
{
    private final ProcResource resource;
    private Schema schema = null;
    private int processorCount = 0;

    public ProcConfig(
            ProcResource resource)
    {
        this.resource = resource;
    }

    public ProcResource getResource()
    {
        return resource;
    }

    public Schema getSchema()
    {
        return schema;
    }

    public ProcConfig setSchema(Schema schema)
    {
        this.schema = schema;
        return this;
    }

    public int getProcessorCount()
    {
        return processorCount;
    }

    public ProcConfig setProcessorCount(int processorCount)
    {
        this.processorCount = processorCount;
        return this;
    }

    public ProcTask getProcTask()
    {
        if (processorCount <= 0) {
            throw new ConfigException("processorCount must be >= 1");
        }
        if (schema == null) {
            throw new ConfigException("schema must not be set");
        }
        return new ProcTask(schema, processorCount);
    }
}
