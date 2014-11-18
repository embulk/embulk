package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.channel.PageInput;

public interface OutputPlugin
{
    public NextConfig runOutputTransaction(ExecTask exec, ConfigSource config,
            ExecControl control);

    public Report runOutput(ExecTask exec, TaskSource taskSource,
            int processorIndex, PageInput pageInput);
}
