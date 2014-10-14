package org.quickload.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.quickload.record.Schema;

public class ProcTask
{
    private final Schema schema;
    private final int processorCount;

    public ProcTask(
            Schema schema,
            int processorCount)
    {
        this.schema = schema;
        this.processorCount = processorCount;
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
