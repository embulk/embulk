package org.quickload.spi;

import org.quickload.config.ConfigSource;

public interface FormatterPlugin
{
    public FormatterTask getFormatterTask(ConfigSource confg, InputTask input);

    public OutputOperator openOperator(FormatterTask task, int processorIndex, BufferOperator op);

    public void shutdown();
}
