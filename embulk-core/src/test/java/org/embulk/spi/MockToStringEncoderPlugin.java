package org.embulk.spi;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.embulk.buffer.Buffer;
import org.embulk.channel.FileBufferInput;
import org.embulk.channel.FileBufferOutput;
import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;

import com.google.common.collect.ImmutableList;

public class MockToStringEncoderPlugin implements FileEncoderPlugin
{
    private List<List<String>> files;

    public interface EncoderPluginTask extends Task
    {
        @Config("prefix")
        public String getPrefix();
    }

    @Override
    public TaskSource getFileEncoderTask(ExecTask exec, ConfigSource config)
    {
        EncoderPluginTask task = exec.loadConfig(config,
                EncoderPluginTask.class);
        return exec.dumpTask(task);
    }

    @Override
    public void runFileEncoder(ExecTask exec, TaskSource taskSource,
            int processorIndex, FileBufferInput fileBufferInput,
            FileBufferOutput fileBufferOutput)
    {
        EncoderPluginTask task = exec.loadTask(taskSource,
                EncoderPluginTask.class);
        files = new ArrayList<>();
        try {
            while (fileBufferInput.nextFile()) {
                List<String> buffers = new ArrayList<>();
                for (Buffer buffer : fileBufferInput) {
                    buffers.add(task.getPrefix()
                            + new String(buffer.get(), "UTF-8"));
                }
                files.add(ImmutableList.copyOf(buffers));
            }
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee.getMessage(), uee);
        }
    }

    public List<List<String>> getFiles()
    {
        return ImmutableList.copyOf(files);
    }
}
