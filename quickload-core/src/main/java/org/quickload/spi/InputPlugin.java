package org.quickload.spi;

import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.channel.PageOutput;

public interface InputPlugin
{
    public NextConfig runInputTransaction(ProcTask proc, ConfigSource config,
            ProcControl control);

    public Report runInput(ProcTask proc, TaskSource taskSource,
            int processorIndex, PageOutput pageOutput);
}
