package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;

public interface FormatterPlugin
{
    public FormatterTask getFormatterTask(ConfigSource config, InputTask input);

    public OutputOperator openFormatterOperator(TaskSource taskSource, int processorIndex, BufferOperator op);

    public void shutdown();
}
