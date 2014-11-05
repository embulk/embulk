package org.quickload.spi;

import java.util.List;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.channel.FileBufferChannel;
import org.quickload.channel.FileBufferOutput;
import org.quickload.channel.PageOutput;

public abstract class FileInputPlugin
        implements InputPlugin
{
    public abstract NextConfig runFileInputTransaction(ProcTask proc, ConfigSource config,
            ProcControl control);

    public abstract Report runFileInput(ProcTask proc, TaskSource taskSource,
            int processorIndex, FileBufferOutput fileBufferOutput);

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
    public NextConfig runInputTransaction(final ProcTask proc, final ConfigSource config,
            final ProcControl control)
    {
        return runFileInputTransaction(proc, config, new ProcControl() {
            public List<Report> run(TaskSource taskSource)
            {
                InputTask task = proc.loadConfig(config, InputTask.class);
                ParserPlugin parser = proc.newPlugin(ParserPlugin.class, task.getParserType());
                task.setParserTask(parser.getParserTask(proc, config));
                task.setFileInputTask(taskSource);
                return control.run(proc.dumpTask(task));
            }
        });
    }

    @Override
    public Report runInput(final ProcTask proc,
            TaskSource taskSource, final int processorIndex,
            final PageOutput pageOutput)
    {
        final InputTask task = proc.loadTask(taskSource, InputTask.class);
        final ParserPlugin parser = proc.newPlugin(ParserPlugin.class, task.getParserType());
        try (final FileBufferChannel channel = proc.newFileBufferChannel()) {
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
