package org.quickload.spi;

import org.quickload.record.Schema;

public interface ParserTask
        extends Task
{
    public Schema getSchema();
}
