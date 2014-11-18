package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.FileBufferOutput;

public interface FileEncoderPlugin
{
    public TaskSource getFileEncoderTask(ExecTask exec, ConfigSource config);

    public void runFileEncoder(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, FileBufferOutput fileBufferOutput);
}
