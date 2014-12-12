package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.channel.FileBufferInput;
import org.embulk.channel.FileBufferOutput;

public interface FileDecoderPlugin
{
    public TaskSource getFileDecoderTask(ExecTask exec, ConfigSource config);

    public void runFileDecoder(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, FileBufferOutput fileBufferOutput);
}
