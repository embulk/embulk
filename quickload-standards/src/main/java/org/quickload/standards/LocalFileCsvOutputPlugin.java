package org.quickload.standards;

import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.DynamicModel;
import org.quickload.exec.BufferManager;
import org.quickload.plugin.PluginManager;
import org.quickload.record.Column;
import org.quickload.record.Page;
import org.quickload.record.PageAllocator;
import org.quickload.record.PageReader;
import org.quickload.record.RecordConsumer;
import org.quickload.record.RecordCursor;
import org.quickload.record.Schema;
import org.quickload.spi.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

public class LocalFileCsvOutputPlugin
        extends FileOutputPlugin<LocalFileCsvOutputPlugin.Task>
{
    @Inject
    public LocalFileCsvOutputPlugin(PluginManager pluginManager) { super(pluginManager); }

    public interface Task
            extends FileOutputTask, DynamicModel<Task>
    {
        @Config("out:paths")
        public List<String> getPaths(); // TODO temporarily

        public Schema getOutputSchema(); // TODO

        @Config("ConfigExpression")
        public String getConfigExpression();

        public FormatterTask getFormatterTask();
    }

    public interface MyFormatterTask
            extends FormatterTask, DynamicModel<MyFormatterTask>
    {
    }

    @Override
    public Task getTask(ConfigSource config, InputTask inputTask)
    {
        Task task = config.load(Task.class);

        MyFormatterTask formatterTask = config.load(MyFormatterTask.class);
        formatterTask.set("Schema", inputTask.getSchema());

        task.set("FormatterTask", formatterTask);

        return task.validate();
    }

    public static class MyBufferOperator
            extends AbstractBufferOperator
    {
        private final Task task;
        private final int processorIndex;

        MyBufferOperator(Task task, int processorIndex) {
            this.task = task;
            this.processorIndex = processorIndex;
        }

        @Override
        public void addBuffer(Buffer buffer) {
            // TODO
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

    @Override
    public BufferOperator openFileOutputOperator(final Task task, final int processorIndex)
    {
        return new MyBufferOperator(task, processorIndex);
    }
}
