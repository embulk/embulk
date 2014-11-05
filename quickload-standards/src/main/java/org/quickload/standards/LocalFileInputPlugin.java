package org.quickload.standards;

import java.io.IOException;
import javax.validation.constraints.NotNull;
import com.google.common.base.Function;
import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.buffer.BufferAllocator;
import org.quickload.config.Config;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.channel.FileBufferOutput;
import org.quickload.plugin.PluginManager;
import org.quickload.record.Schema;
import org.quickload.spi.FileInputPlugin;
import org.quickload.spi.ProcTask;
import org.quickload.spi.ProcControl;

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
    public NextConfig runFileInputTransaction(ProcTask proc, ConfigSource config,
            ProcControl control)
    {
        PluginTask task = proc.loadConfig(config, PluginTask.class);
        proc.setProcessorCount(task.getPaths().size());

        control.run(proc.dumpTask(task));

        return new NextConfig();
    }

    @Override
    public Report runFileInput(ProcTask proc, TaskSource taskSource,
            int processorIndex, FileBufferOutput fileBufferOutput)
    {
        PluginTask task = proc.loadTask(taskSource, PluginTask.class);
        BufferAllocator bufferAllocator = proc.getBufferAllocator();

        // TODO ad-hoc
        String path = task.getPaths().get(processorIndex);

        try {
            File file = new File(path);
            byte[] bytes = new byte[1024];
            Buffer buf = bufferAllocator.allocateBuffer(1024);

            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                int len;
                while ((len = in.read(bytes)) >= 0) {
                    buf.limit(buf.limit() + len);
                    if (buf.capacity() - buf.limit() < 1024) {
                        fileBufferOutput.add(buf);
                        buf = bufferAllocator.allocateBuffer(1024);
                    }
                }
                if (buf.limit() > 0) {
                    fileBufferOutput.add(buf);
                }
            }
            fileBufferOutput.addFile();

        } catch (IOException e) {
            throw new RuntimeException(e);  // TODO
        }

        return new Report();
    }
}
