package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;

public interface ParserPlugin
{
    public ParserTask getParserTask(ConfigSource config);

    public BufferOperator openParserOperator(TaskSource taskSource, int processorIndex, OutputOperator op);

    public void shutdown();
}
