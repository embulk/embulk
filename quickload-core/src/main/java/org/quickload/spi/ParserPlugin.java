package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.config.Report;
import org.quickload.queue.BufferInput;
import org.quickload.queue.PageOutput;

public interface ParserPlugin
{
    public TaskSource getParserTask(ProcConfig proc, ConfigSource config);

    public Future<Report> startParser(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            BufferInput bufferInput, PageOutput pageOutput);
}
