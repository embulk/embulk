package org.quickload.spi;

import org.quickload.config.ConfigSource;

public interface FormatterPlugin<T extends FormatterTask>
{
    public OutputOperator openOperator(T task, int processorIndex, BufferOperator op);

    public void shutdown();
}
