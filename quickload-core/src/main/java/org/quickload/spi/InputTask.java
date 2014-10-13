package org.quickload.spi;

import org.quickload.record.Schema;

public interface InputTask
        extends Task
{
    public Schema getSchema();

    public int getProcessorCount();
}
