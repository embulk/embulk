package org.quickload.standards;

import javax.validation.constraints.NotNull;
import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.config.Config;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.plugin.PluginManager;
import org.quickload.record.Schema;
import org.quickload.spi.BufferOperator;
import org.quickload.spi.FileOutputPlugin;
import org.quickload.spi.ProcTask;
import org.quickload.spi.Report;
import org.quickload.spi.FailedReport;

import java.util.List;

public class LocalFileOutputPlugin
        extends FileOutputPlugin
{
    @Inject
    public LocalFileOutputPlugin(PluginManager pluginManager)
    {
        super(pluginManager);
    }

    public interface PluginTask
            extends Task
    {
        @Config("out:paths")
        @NotNull
        public List<String> getPaths(); // TODO temporarily
    }

    @Override
    public TaskSource getFileOutputTask(ProcTask proc, ConfigSource config)
    {
        PluginTask task = config.loadTask(PluginTask.class);
        return config.dumpTask(task);
    }

    @Override
    public BufferOperator openBufferOutputOperator(ProcTask proc,
            TaskSource taskSource, int processorIndex)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        return new PluginOperator(task, processorIndex);
    }

    // TODO can be PluginOperator ported to standard library?
    public static class PluginOperator
            implements BufferOperator
    {
        private final PluginTask task;
        private final int processorIndex;

        PluginOperator(PluginTask task, int processorIndex)
        {
            this.task = task;
            this.processorIndex = processorIndex;
        }

        @Override
        public void addBuffer(Buffer buffer) {
            // TODO write buffer to local files
            List<String> paths = task.getPaths();
            System.out.println(new String(buffer.get()));
        }

        @Override
        public Report failed(Exception cause)
        {
            // TODO
            return new FailedReport(null, null);
        }

        @Override
        public Report completed() {
            return null; // TODO
        }

        @Override
        public void close() throws Exception {
            // TODO
        }
    }
}
