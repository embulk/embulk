package org.quickload.spi;

import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.Report;
import org.quickload.channel.PageOutput;

public interface InputPlugin
{
    public TaskSource getInputTask(ProcTask proc, ConfigSource config);

    public void runInputTransaction(ProcTask proc, TaskSource taskSource,
            ProcControl control);

    public Report runInput(ProcTask proc, TaskSource taskSource,
            int processorIndex, PageOutput pageOutput);
}
