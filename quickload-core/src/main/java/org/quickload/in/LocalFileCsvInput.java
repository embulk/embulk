package org.quickload.in;

import java.util.List;
import java.util.ArrayList;

import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.DynamicModel;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
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
    public interface Task
            extends InputTask, DynamicModel<Task>
    {
        @Config("paths")
        public List<String> getPaths();

        @Min(1)
        public int getProcessorCount();
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
        Task task = config.load(Task.class);
        //task.getBasePath()
        //task.set("paths", ...);
        //return task.validate();
        task.set("ProcessorCount", task.getPaths().size());
        return task.validate();
    }

    @Override
    public Processor startInputProcessor(Task task,
            int processorIndex, OutputOperator op)
    {
        return new Processor(task, processorIndex, op);
    }
}
