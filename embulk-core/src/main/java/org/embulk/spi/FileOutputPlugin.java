package org.embulk.spi;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
import org.embulk.channel.FileBufferChannel;
import org.embulk.channel.FileBufferInput;
import org.embulk.channel.PageInput;

public abstract class FileOutputPlugin
        implements OutputPlugin
{
    public abstract NextConfig runFileOutputTransaction(ExecTask exec, ConfigSource config,
            ExecControl control);

    public abstract Report runFileOutput(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput);

    public interface OutputTask
            extends Task
    {
        @Config("formatter")
        @NotNull
        public ConfigSource getFormatterConfig();

        public TaskSource getFormatterTask();
        public void setFormatterTask(TaskSource task);

        public TaskSource getFileOutputTask();
        public void setFileOutputTask(TaskSource task);
    }

    protected FormatterPlugin newFormatterPlugin(ExecTask exec, OutputTask task)
    {
        return exec.newPlugin(FormatterPlugin.class, task.getFormatterConfig().get("type"));
    }

    @Override
    public NextConfig runOutputTransaction(final ExecTask exec, ConfigSource config,
            final ExecControl control)
    {
        final OutputTask task = exec.loadConfig(config, OutputTask.class);

        return runFileOutputTransaction(exec, config, new ExecControl() {
            public List<Report> run(TaskSource taskSource)
            {
                FormatterPlugin formatter = newFormatterPlugin(exec, task);
                task.setFormatterTask(formatter.getFormatterTask(exec, task.getFormatterConfig()));
                task.setFileOutputTask(taskSource);
                return control.run(exec.dumpTask(task));
            }
        });
    }

    @Override
    public Report runOutput(final ExecTask exec,
            TaskSource taskSource, final int processorIndex,
            final PageInput pageInput)
    {
        final OutputTask task = exec.loadTask(taskSource, OutputTask.class);
        final FormatterPlugin formatter = newFormatterPlugin(exec, task);

        PluginThread thread = null;
        Throwable error = null;
        try (final FileBufferChannel channel = exec.newFileBufferChannel()) {
            thread = exec.startPluginThread(new Runnable() {
                public void run()
                {
                    try {
                        formatter.runFormatter(exec,
                                task.getFormatterTask(), processorIndex,
                                pageInput, channel.getOutput());
                    } finally {
                        channel.completeProducer();
                    }
                }
            });

            Report report = runFileOutput(exec,
                    task.getFileOutputTask(), processorIndex,
                    channel.getInput());
            channel.completeConsumer();
            thread.join();
            channel.join();  // throws if channel is not fully consumed

            return report;  // TODO merge formatterReport

        } catch (Throwable ex) {
            error = ex;
            return null;  // finally block throws an exception
        } finally {
            PluginThread.joinAndThrowNested(thread, error);
        }
    }
}
