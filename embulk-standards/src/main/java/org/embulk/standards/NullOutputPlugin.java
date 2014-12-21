package org.embulk.standards;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
import org.embulk.channel.PageInput;
import org.embulk.page.Page;
import org.embulk.spi.ExecTask;
import org.embulk.spi.ExecControl;
import org.embulk.spi.OutputPlugin;

public class NullOutputPlugin
        implements OutputPlugin
{
    public NextConfig runOutputTransaction(ExecTask exec, ConfigSource config,
            ExecControl control)
    {
        control.run(new TaskSource());
        return new NextConfig();
    }

    public Report runOutput(ExecTask exec, TaskSource taskSource,
            int processorIndex, PageInput pageInput)
    {
        for (Page page : pageInput) {
            page.release();
        }
        return new Report();
    }
}
