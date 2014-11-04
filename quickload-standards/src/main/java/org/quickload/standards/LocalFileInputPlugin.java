package org.quickload.standards;

import javax.validation.constraints.NotNull;
import com.google.common.base.Function;
import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.buffer.BufferAllocator;
import org.quickload.config.Config;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.Report;
import org.quickload.channel.FileBufferOutput;
import org.quickload.plugin.PluginManager;
import org.quickload.record.Schema;
import org.quickload.spi.FileInputPlugin;
import org.quickload.spi.ProcTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class LocalFileInputPlugin
        extends FileInputPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("in:paths") // TODO temporarily added 'in:'
        @NotNull
        public List<String> getPaths();
    }

    @Override
    public TaskSource getFileInputTask(ProcTask proc, ConfigSource config)
    {
        PluginTask task = config.loadTask(PluginTask.class);
        proc.setProcessorCount(task.getPaths().size());
        return config.dumpTask(task);
    }

    @Override
    public Report runFileInput(ProcTask proc, TaskSource taskSource,
            int processorIndex, FileBufferOutput fileBufferOutput)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        BufferAllocator bufferAllocator = proc.getBufferAllocator();

        // TODO ad-hoc
        String path = task.getPaths().get(processorIndex);

        try {
            File file = new File(path);
            byte[] bytes = new byte[1024];
            Buffer buf = bufferAllocator.allocateBuffer(128*1024); // TODO

            int len = 0, offset = 0;
            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                while ((len = in.read(bytes)) > 0) {
                    int rest = buf.capacity() - offset;
                    if (rest >= len) {
                        buf.write(bytes, 0, len);
                        offset += len;
                    } else {
                        buf.write(bytes, 0, rest);
                        buf.flush();
                        fileBufferOutput.add(buf);
                        offset = 0;

                        buf = bufferAllocator.allocateBuffer(128*1024); // TODO
                        buf.write(bytes, rest, len - rest);
                        offset += len - rest;
                    }
                }

                if (offset > 0) {
                    buf.flush();
                    fileBufferOutput.add(buf);
                }
            }

            fileBufferOutput.addFile();

        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO
        }

        return new Report();
    }
}
