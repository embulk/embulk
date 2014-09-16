package org.quickload.in;

import java.util.List;
import java.util.ArrayList;

import org.quickload.config.ConfigSource;
import org.quickload.spi.BasicInputPlugin;
import org.quickload.spi.InputTask;
import org.quickload.spi.Report;
import org.quickload.spi.ReportBuilder;
import org.quickload.spi.InputProgress;
import org.quickload.spi.ThreadInputProcessor;
import org.quickload.spi.OutputOperator;
import org.quickload.spi.DynamicReport;

public class LocalFileCsvInput
        extends BasicInputPlugin<LocalFileCsvInput.Task>
{
    public static class Task
            implements InputTask
    {
        private List<String> paths;

        Task()
        {
            paths = new ArrayList();
            paths.add("test.csv");
        }

        public List<String> getPaths()
        {
            return paths;
        }

        @Override
        public int getProcessorCount()
        {
            return paths.size();
        }
    }

    public static class Processor
            extends ThreadInputProcessor
    {
        private final Task task;
        private final int processorIndex;

        public Processor(Task task,
                int processorIndex, OutputOperator op)
        {
            super(op);
            this.task = task;
            this.processorIndex = processorIndex;
        }

        @Override
        public ReportBuilder runThread() throws Exception
        {
            String path = task.getPaths().get(processorIndex);
            // ...
            return DynamicReport.builder();
        }

        @Override
        public InputProgress getProgress()
        {
            return null;
        }
    }

    @Override
    public Task getInputTask(ConfigSource config)
    {
        //Task b = config.configure(Task.class);
        //b.getBasePath()
        //b.set("paths", ...);
        //return b.validate();
        return new Task();
    }

    @Override
    public Processor startInputProcessor(Task task,
            int processorIndex, OutputOperator op)
    {
        return new Processor(task, processorIndex, op);
    }
}
