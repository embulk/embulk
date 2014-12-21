package org.embulk.spi;

import java.util.List;
import java.util.ArrayList;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
import org.embulk.type.Schema;

public class FileOutputRunner
        implements OutputPlugin
{
    private interface RunnerTask extends Task
    {
        @Config("type")
        public JsonNode getType();

        @Config("encoders")
        @ConfigDefault("[]")
        public List<ConfigSource> getEncoderConfigs();

        @Config("formatter")
        public ConfigSource getFormatterConfig();

        public void setFileOutputTaskSource(TaskSource v);
        public TaskSource getFileOutputTaskSource();

        public void setEncoderTaskSources(List<TaskSource> v);
        public List<TaskSource> getEncoderTaskSources();

        public void setFormatterTaskSource(TaskSource v);
        public TaskSource getFormatterTaskSource();
    }

    protected FileOutputPlugin newFileOutputPlugin(RunnerTask task)
    {
        return Exec.newPlugin(FileOutputPlugin.class, task.getType());
    }

    protected List<EncoderPlugin> newEncoderPlugins(RunnerTask task)
    {
        return Encoders.newEncoderPlugins(Exec.session(), task.getEncoderConfigs());
    }

    protected FormatterPlugin newFormatterPlugin(RunnerTask task)
    {
        return Exec.newPlugin(FormatterPlugin.class, task.getFormatterConfig().get("type"));
    }

    @Override
    public NextConfig transaction(ConfigSource config,
            final Schema schema, final int processorCount,
            final OutputPlugin.Control control)
    {
        final RunnerTask task = config.loadConfig(RunnerTask.class);
        FileOutputPlugin fileOutputPlugin = newFileOutputPlugin(task);
        final List<EncoderPlugin> encoderPlugins = newEncoderPlugins(task);
        final FormatterPlugin formatterPlugin = newFormatterPlugin(task);

        return fileOutputPlugin.transaction(config, processorCount, new FileOutputPlugin.Control() {
            public List<CommitReport> run(final TaskSource fileOutputTaskSource)
            {
                final List<CommitReport> commitReports = new ArrayList<CommitReport>();
                Encoders.transaction(encoderPlugins, task.getEncoderConfigs(), new Encoders.Control() {
                    public void run(final List<TaskSource> encoderTaskSources)
                    {
                        formatterPlugin.transaction(task.getFormatterConfig(), schema, new FormatterPlugin.Control() {
                            public void run(final TaskSource formatterTaskSource)
                            {
                                TaskSource taskSource = new TaskSource();
                                task.setFileOutputTaskSource(fileOutputTaskSource);
                                task.setEncoderTaskSources(encoderTaskSources);
                                task.setFormatterTaskSource(formatterTaskSource);
                                commitReports.addAll(control.run(Exec.dumpTask(task)));
                            }
                        });
                    }
                });
                return commitReports;
            }
        });
    }

    @Override
    public TransactionalPageOutput open(TaskSource taskSource, Schema schema, int processorIndex)
    {
        final RunnerTask task = taskSource.loadTask(RunnerTask.class);
        FileOutputPlugin fileOutputPlugin = newFileOutputPlugin(task);
        List<EncoderPlugin> encoderPlugins = newEncoderPlugins(task);
        FormatterPlugin formatterPlugin = newFormatterPlugin(task);

        Transactional tran = null;
        FileOutput fileOutput = null;
        PageOutput output = null;
        try {
            fileOutput = fileOutputPlugin.open(taskSource, processorIndex);
            tran = (Transactional) fileOutput;

            fileOutput = Encoders.open(encoderPlugins, task.getEncoderTaskSources(), tran);
            output = formatterPlugin.open(taskSource, schema, fileOutput);

            TransactionalPageOutput ret = new DelegateTransactionalPageOutput(tran, output);
            tran = null;
            output = null;
            return ret;

        } finally {
            if (output != null) {
                output.close();
            }
            if (fileOutput != null) {
                fileOutput.close();
                fileOutput = null;
            }
            if (tran != null) {
                tran.abort();
            }
        }
    }

    private static class DelegateTransactionalPageOutput
    {
        private final Transactional tran;
        private final PageOutput output;

        public DelegateTransactionalPageOutput(Transactional tran, PageOutput output)
        {
            this.tran = tran;
            this.output = output;
        }

        @Override
        public void add(Page page)
        {
            output.add(page);
        }

        @Override
        public void close()
        {
            output.close();
        }

        @Override
        public void abort()
        {
            tran.abort();
        }

        @Override
        public CommitReport commit()
        {
            return tran.commit();
        }
    }
}
