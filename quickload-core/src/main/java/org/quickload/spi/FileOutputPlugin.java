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
        @Config("out:formatter_type")
        @NotNull
        public JsonNode getFormatterType();

        public TaskSource getFormatterTask();
        public void setFormatterTask(TaskSource task);

        public TaskSource getFileOutputTask();
        public void setFileOutputTask(TaskSource task);
    }

    @Override
    public NextConfig runOutputTransaction(final ProcTask proc, final ConfigSource config,
            final ProcControl control)
    {
        return runFileOutputTransaction(proc, config, new ProcControl() {
            public List<Report> run(TaskSource taskSource)
            {
                OutputTask task = proc.loadConfig(config, OutputTask.class);
                FormatterPlugin formatter = proc.newPlugin(FormatterPlugin.class, task.getFormatterType());
                task.setFormatterTask(formatter.getFormatterTask(proc, config));
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
        final OutputTask task = taskSource.loadTask(OutputTask.class);
        final FormatterPlugin formatter = proc.newPlugin(FormatterPlugin.class, task.getFormatterType());
        try (final FileBufferChannel channel = proc.newFileBufferChannel()) {
            proc.startPluginThread(new PluginThread() {
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

            Report report = runFileOutput(proc,
                    task.getFileOutputTask(), processorIndex,
                    channel.getInput());
            channel.completeProducer();
            channel.join();

            return report;  // TODO merge formatterReport
        }
    }
}
