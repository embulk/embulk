package org.quickload.spi;

import org.quickload.record.Schema;

public interface FormatterTask
        extends Task
{
    public Schema getSchema();
}
