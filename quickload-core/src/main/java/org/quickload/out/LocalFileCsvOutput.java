package org.quickload.out;

import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.DynamicModel;
import org.quickload.record.Page;
import org.quickload.spi.*;

import java.util.List;

public class LocalFileCsvOutput
        extends BasicOutputPlugin<LocalFileCsvOutput.Task>
{
    // TODO use DynamicTask
    public interface Task
            extends OutputTask, DynamicModel<Task>
    {
        @Config("paths")
        public List<String> getPaths();
    }

    public static class Operator
            extends AbstractOutputOperator
    {
        private final Task task;
        private final int processorIndex;

        Operator(Task task, int processorIndex) {
            this.task = task;
            this.processorIndex = processorIndex;
        }

        @Override
        public void addPage(Page page)
        {
            int len = page.length();
            // ...
        }

        @Override
        public void close() throws Exception
        {
        }

        @Override
        public Report completed()
        {
            return DynamicReport.builder().build(null);
        }
    }

    @Override
    public Task getTask(ConfigSource config, InputTask input)
    {
        Task task = config.load(Task.class);
        return task.validate();
    }

    @Override
    public void begin(Task task)
    {
    }

    @Override
    public Operator openOperator(Task task, int processorIndex)
    {
        return new Operator(task, processorIndex);
    }

    @Override
    public void commit(Task task, List<Report> reports)
    {
    }

    @Override
    public void abort(Task task)
    {
    }
}
