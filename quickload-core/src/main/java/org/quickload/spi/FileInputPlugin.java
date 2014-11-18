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
    public abstract NextConfig runFileInputTransaction(ExecTask exec, ConfigSource config,
            ExecControl control);

    public abstract Report runFileInput(ExecTask exec, TaskSource taskSource,
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

    protected ParserPlugin newParserPlugin(ExecTask exec, InputTask task)
    {
        return exec.newPlugin(ParserPlugin.class, task.getParserConfig().get("type"));
    }

    @Override
    public NextConfig runInputTransaction(final ExecTask exec, ConfigSource config,
            final ExecControl control)
    {
        final InputTask task = exec.loadConfig(config, InputTask.class);

        return runFileInputTransaction(exec, config, new ExecControl() {
            public List<Report> run(TaskSource taskSource)
            {
                ParserPlugin parser = newParserPlugin(exec, task);
                task.setParserTask(parser.getParserTask(exec, task.getParserConfig()));
                task.setFileInputTask(taskSource);
                return control.run(exec.dumpTask(task));
            }
        });
    }

    @Override
    public Report runInput(final ExecTask exec,
            TaskSource taskSource, final int processorIndex,
            final PageOutput pageOutput)
    {
        final InputTask task = exec.loadTask(taskSource, InputTask.class);
        final ParserPlugin parser = newParserPlugin(exec, task);

        PluginThread thread = null;
        Throwable error = null;
        try (final FileBufferChannel channel = exec.newFileBufferChannel()) {
            thread = exec.startPluginThread(new Runnable() {
                public void run()
                {
                    try {
                        parser.runParser(exec,
                                task.getParserTask(), processorIndex,
                                channel.getInput(), pageOutput);
                    } finally {
                        channel.completeConsumer();
                    }
                }
            });

            Report report = runFileInput(exec,
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
