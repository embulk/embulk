package org.quickload.spi;

import java.util.concurrent.Future;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.config.Report;
import org.quickload.channel.BufferChannel;
import org.quickload.channel.BufferOutput;
import org.quickload.channel.PageOutput;

public abstract class FileInputPlugin
        implements InputPlugin
{
    public abstract TaskSource getFileInputTask(ProcTask proc, ConfigSource config);

    @Override
    public void runInputTransaction(ProcTask proc, TaskSource taskSource,
            ProcControl control)
    {
        control.run();
    }

    public abstract Report runFileInput(ProcTask proc, TaskSource taskSource,
            int processorIndex, BufferOutput bufferOutput);

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
    public TaskSource getInputTask(ProcTask proc, ConfigSource config)
    {
        InputTask task = config.loadTask(InputTask.class);
        ParserPlugin parser = proc.newPlugin(ParserPlugin.class, task.getParserType());
        task.setParserTask(parser.getParserTask(proc, config));
        task.setFileInputTask(getFileInputTask(proc, config));
        return config.dumpTask(task);
    }

    @Override
    public Report runInput(final ProcTask proc,
            TaskSource taskSource, final int processorIndex,
            final PageOutput pageOutput)
    {
        final InputTask task = taskSource.loadTask(InputTask.class);
        final ParserPlugin parser = proc.newPlugin(ParserPlugin.class, task.getParserType());
        try (final BufferChannel channel = proc.newBufferChannel()) {
            proc.startPluginThread(new PluginThread() {
                public void run()
                {
                    try {
                        parser.runParser(proc,
                                task.getParserTask(), processorIndex,
                                channel.getInput(), pageOutput);
                    } finally {
                        channel.completeConsumer();
                    }
                }
            });

            Report report = runFileInput(proc,
                    task.getFileInputTask(), processorIndex,
                    channel.getOutput());
            channel.completeProducer();
            channel.join();

            return report;
        }
    }
}
