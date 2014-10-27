package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.config.Report;
import org.quickload.queue.PageInput;
import org.quickload.queue.BufferOutput;

public interface FormatterPlugin
{
    public TaskSource getFormatterTask(ProcTask proc, ConfigSource config);

    public Report runFormatter(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            PageInput pageInput, BufferOutput bufferOutput);
}
