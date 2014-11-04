package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.channel.PageInput;

public interface OutputPlugin
{
    public NextConfig runOutputTransaction(ProcTask proc, ConfigSource config,
            ProcControl control);

    public Report runOutput(ProcTask proc, TaskSource taskSource,
            int processorIndex, PageInput pageInput);
}
