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
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.PageInput;

public abstract class FileOutputPlugin
        implements OutputPlugin
{
    public abstract NextConfig runFileOutputTransaction(ProcTask proc, ConfigSource config,
            ProcControl control);

    public abstract Report runFileOutput(ProcTask proc,
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

    protected FormatterPlugin newFormatterPlugin(ProcTask proc, OutputTask task)
    {
        return proc.newPlugin(FormatterPlugin.class, task.getFormatterConfig().get("type"));
    }

    @Override
    public NextConfig runOutputTransaction(final ProcTask proc, ConfigSource config,
            final ProcControl control)
    {
        final OutputTask task = proc.loadConfig(config, OutputTask.class);

        return runFileOutputTransaction(proc, config, new ProcControl() {
            public List<Report> run(TaskSource taskSource)
            {
                FormatterPlugin formatter = newFormatterPlugin(proc, task);
                task.setFormatterTask(formatter.getFormatterTask(proc, task.getFormatterConfig()));
                task.setFileOutputTask(taskSource);
                return control.run(proc.dumpTask(task));
            }
        });
    }

    @Override
    public Report runOutput(final ProcTask proc,
            TaskSource taskSource, final int processorIndex,
            final PageInput pageInput)
    {
        final OutputTask task = proc.loadTask(taskSource, OutputTask.class);
        final FormatterPlugin formatter = newFormatterPlugin(proc, task);

        try (final FileBufferChannel channel = proc.newFileBufferChannel()) {
            PluginThread thread = proc.startPluginThread(new Runnable() {
                public void run()
                {
                    try {
                        formatter.runFormatter(proc,
                                task.getFormatterTask(), processorIndex,
                                pageInput, channel.getOutput());
                    } finally {
                        channel.completeConsumer();
                    }
                }
            });

            try {
                Report report = runFileOutput(proc,
                        task.getFileOutputTask(), processorIndex,
                        channel.getInput());
                channel.completeProducer();
                channel.join();

                return report;  // TODO merge formatterReport
            } finally {
                thread.joinAndThrow();
            }
        }
    }
}
