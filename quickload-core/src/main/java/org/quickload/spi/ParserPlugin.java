package org.quickload.spi;

import org.quickload.config.ConfigSource;

public interface ParserPlugin
{
    public ParserTask getParserTask(ConfigSource config);

    public BufferOperator openOperator(ParserTask task, int processorIndex, OutputOperator op);

    public void shutdown();
}
