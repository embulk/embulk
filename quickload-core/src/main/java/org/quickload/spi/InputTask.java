package org.quickload.spi;

import javax.validation.constraints.Min;

public interface InputTask
{
    // TODO getSchema

    @Min(1)
    public int getProcessorCount();
}
