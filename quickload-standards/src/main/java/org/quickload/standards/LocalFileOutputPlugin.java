package org.quickload.standards;

import javax.validation.constraints.NotNull;
import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.plugin.PluginManager;
import org.quickload.record.Schema;
import org.quickload.spi.BufferOperator;
import org.quickload.spi.FileOutputPlugin;
import org.quickload.spi.FileOutputTask;
import org.quickload.spi.FormatterTask;
import org.quickload.spi.InputTask;
import org.quickload.spi.Report;
import org.quickload.spi.FailedReport;

import java.util.List;

public class LocalFileOutputPlugin
        extends FileOutputPlugin<LocalFileOutputPlugin.Task>
{
    @Inject
    public LocalFileOutputPlugin(PluginManager pluginManager) {
        super(pluginManager);
    }

    public interface Task
            extends FileOutputTask
    {
        @Config("out:paths")
        @NotNull
        public List<String> getPaths(); // TODO temporarily
    }

    @Override
    public Task getFileOutputTask(ConfigSource config, InputTask input,
            FormatterTask formatterTask)
    {
        Task task = config.load(Task.class);
        task.setFormatterTask(config.dumpTask(formatterTask));
        task.validate();
        return task;
    }

    @Override
    public BufferOperator openFileOutputOperator(final Task task, final int processorIndex)
    {
        return new Operator(task, processorIndex);
    }

    // TODO can be Operator ported to standard library?
    public class Operator
            implements BufferOperator
    {
        private final Task task;
        private final int processorIndex;

        Operator(Task task, int processorIndex) {
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
