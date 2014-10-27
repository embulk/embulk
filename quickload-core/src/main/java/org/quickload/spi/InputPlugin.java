package org.quickload.spi;

import org.quickload.config.TaskSource;
import org.quickload.config.Report;
import org.quickload.queue.PageOutput;

public interface InputPlugin
{
    public TaskSource getInputTask(ProcConfig proc, ConfigSource config);

    public void runInputTransaction(ProcTask proc,
            ProcControl control, TaskSource taskSource)

    public Report runInput(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            PageOutput pageOutput);
}
