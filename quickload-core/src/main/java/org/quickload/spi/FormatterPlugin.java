package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;

public interface FormatterPlugin
{
    public TaskSource getFormatterTask(ProcTask proc, ConfigSource config);

    public PageOperator openPageOperator(ProcTask proc,
            TaskSource taskSource, int processorIndex, BufferOperator next);

    public void shutdown();
}
