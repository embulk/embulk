package org.quickload.spi;

import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.config.Report;
import org.quickload.queue.BufferOutput;

public abstract class FileInputPlugin
        extends InputPlugin
{
    public abstract TaskSource getFileInputTask(ProcConfig proc, ConfigSource config);

    public abstract Report runFileInput(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            BufferOutput bufferOutput);

    public interface InputTask
            extends Task
    {
        @Config("in:parser_type") // TODO temporarily added 'in:'
        @NotNull
        public JsonNode getParserType();

        public TaskSource getParserTask();
        public void setParserTask(TaskSource source);

        public TaskSource getFileInputTask();
        public void setFileInputTask(TaskSource task);
    }

    @Override
    public TaskSource getInputTask(ProcConfig proc, ConfigSource config)
    {
        InputTask task = config.loadTask(InputTask.class);
        ParserPlugin parser = proc.getResource().newPlugin(ParserPlugin.class, task.getParserType());
        task.setParserTask(parser.getParserTask(proc, config));
        task.setFileInputTask(getFileInputTask(proc, config));
        return config.dumpTask(task);
    }

    @Override
    public void runInputTransaction(ProcTask proc,
            ProcControl control, TaskSource taskSource)
    {
        control.run();
    }

    public Report runInput(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            PageOutput pageOutput)
    {
        InputTask task = taskSource.loadTask(InputTask.class);
        ParserPlugin parser = proc.getResource().newPlugin(ParserPlugin.class, task.getParserType());
        BufferQueue queue = proc.getResource().newBufferQueue();
        parser.startParser(proc,
                task.getParserTask, processorIndex,
                queue.getBufferInput(), pageOutput);
        return runFileInput(proc,
                task.getFileInputTask(), processorIndex,
                queue.getBufferOutput());
    }
}
