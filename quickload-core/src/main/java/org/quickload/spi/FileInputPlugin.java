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
        @Config("parser")
        @NotNull
        public ConfigSource getParserConfig();

        public TaskSource getParserTask();
        public void setParserTask(TaskSource source);

        public TaskSource getFileInputTask();
        public void setFileInputTask(TaskSource task);
    }

    protected ParserPlugin newParserPlugin(ProcTask proc, InputTask task)
    {
        return proc.newPlugin(ParserPlugin.class, task.getParserConfig().get("type"));
    }

    @Override
    public NextConfig runInputTransaction(final ProcTask proc, ConfigSource config,
            final ProcControl control)
    {
        final InputTask task = proc.loadConfig(config, InputTask.class);

        return runFileInputTransaction(proc, config, new ProcControl() {
            public List<Report> run(TaskSource taskSource)
            {
                ParserPlugin parser = newParserPlugin(proc, task);
                task.setParserTask(parser.getParserTask(proc, task.getParserConfig()));
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
        final ParserPlugin parser = newParserPlugin(proc, task);

        PluginThread thread = null;
        Throwable error = null;
        try (final FileBufferChannel channel = proc.newFileBufferChannel()) {
            thread = proc.startPluginThread(new Runnable() {
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
            thread.join();
            channel.join();  // throws if channel is not fully consumed

            return report;

        } catch (Throwable ex) {
            error = ex;
            return null;  // finally block throws an exception
        } finally {
            PluginThread.joinAndThrowNested(thread, error);
        }
    }
}
