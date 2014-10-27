package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.config.Report;
import org.quickload.queue.PageInput;

public interface OutputPlugin
{
    public TaskSource getOutputTask(ProcTask proc, ConfigSource config);

    public void runOutputTransaction(ProcTask proc,
            ProcControl progress, TaskSource taskSource);

    public Report runOutput(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            PageInput pageInput);
}
