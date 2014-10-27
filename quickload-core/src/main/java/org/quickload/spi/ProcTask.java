package org.quickload.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.quickload.record.Schema;

public class ProcTask
{
    private final ProcResource resource;
    private final Schema schema;
    private final int processorCount;

    public ProcTask(
            ProcResource resource,
            Schema schema,
            int processorCount)
    {
        this.resource = resource;
        this.schema = schema;
        this.processorCount = processorCount;
    }

    public ProcResource getResource()
    {
        return resource;
    }

    public Schema getSchema()
    {
        return schema;
    }

    public int getProcessorCount()
    {
        return processorCount;
    }
}
