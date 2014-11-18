package org.quickload.spi;

import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.channel.PageOutput;

public interface InputPlugin
{
    public NextConfig runInputTransaction(ExecTask exec, ConfigSource config,
            ExecControl control);

    public Report runInput(ExecTask exec, TaskSource taskSource,
            int processorIndex, PageOutput pageOutput);
}
