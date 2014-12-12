package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
import org.embulk.channel.PageInput;

public interface OutputPlugin
{
    public NextConfig runOutputTransaction(ExecTask exec, ConfigSource config,
            ExecControl control);

    public Report runOutput(ExecTask exec, TaskSource taskSource,
            int processorIndex, PageInput pageInput);
}
