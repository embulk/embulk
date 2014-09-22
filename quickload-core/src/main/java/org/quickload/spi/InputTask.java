package org.quickload.spi;

import org.quickload.record.Schema;

public interface InputTask
{
    public Schema getSchema();

    public int getProcessorCount();
}
