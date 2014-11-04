package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.PageOutput;

public interface FileParserPlugin
{
    public TaskSource getFileParserTask(ProcTask proc, ConfigSource config);

    public void runFileParser(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, PageOutput pageOutput);
}
