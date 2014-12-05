package org.quickload.spi;

import java.io.UnsupportedEncodingException;

import org.quickload.buffer.Buffer;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.FileBufferOutput;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;

// This decoder adds specified String to all buffers.
// TODO: make it more "decoder"-ish.
public class MockDecoderPlugin implements FileDecoderPlugin
{
    public interface DecoderPluginTask extends Task
    {
        @Config("postfix")
        public String getPostfix();
    }

    @Override
    public TaskSource getFileDecoderTask(ExecTask exec, ConfigSource config)
    {
        DecoderPluginTask task = exec.loadConfig(config,
                DecoderPluginTask.class);
        return exec.dumpTask(task);
    }

    @Override
    public void runFileDecoder(ExecTask exec, TaskSource taskSource,
            int processorIndex, FileBufferInput fileBufferInput,
            FileBufferOutput fileBufferOutput)
    {
        DecoderPluginTask task = exec.loadTask(taskSource,
                DecoderPluginTask.class);
        try {
            while (fileBufferInput.nextFile()) {
                for (Buffer buffer : fileBufferInput) {
                    StringBuilder sb = new StringBuilder().append(buffer.get())
                            .append(task.getPostfix());
                    fileBufferOutput.add(Buffer.copyOf(sb.toString().getBytes(
                            "UTF-8")));
                }
//                for (Buffer buffer : fileBufferInput) { }
            }
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee.getMessage(), uee);
        }
    }
}