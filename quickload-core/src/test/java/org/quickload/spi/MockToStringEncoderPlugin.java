package org.quickload.spi;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.quickload.buffer.Buffer;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.FileBufferOutput;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;

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
