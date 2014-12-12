package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.channel.PageInput;
import org.embulk.channel.FileBufferOutput;

public interface FormatterPlugin
{
    public TaskSource getFormatterTask(ExecTask exec, ConfigSource config);

    public void runFormatter(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            PageInput pageInput, FileBufferOutput fileBufferOutput);
}
