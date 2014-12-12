package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.channel.FileBufferInput;
import org.embulk.channel.PageOutput;

public interface ParserPlugin
{
    public TaskSource getParserTask(ExecTask exec, ConfigSource config);

    public void runParser(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, PageOutput pageOutput);
}
