package org.embulk.spi;

import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
import org.embulk.channel.PageOutput;

public interface InputPlugin
{
    public NextConfig runInputTransaction(ExecTask exec, ConfigSource config,
            ExecControl control);

    public Report runInput(ExecTask exec, TaskSource taskSource,
            int processorIndex, PageOutput pageOutput);
}
