package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.channel.BufferInput;
import org.quickload.channel.PageOutput;

public interface ParserPlugin
{
    public TaskSource getParserTask(ProcTask proc, ConfigSource config);

    public void runParser(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            BufferInput bufferInput, PageOutput pageOutput);
}
