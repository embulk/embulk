package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;

public interface ParserPlugin
{
    public TaskSource getParserTask(ProcConfig proc, ConfigSource config);

    public BufferOperator openBufferOperator(ProcTask proc,
            TaskSource taskSource, int processorIndex, PageOperator next);

    public void shutdown();
}
