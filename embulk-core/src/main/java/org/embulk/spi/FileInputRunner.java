package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.CommitReport;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.plugin.PluginType;
import org.embulk.spi.util.Decoders;
import org.embulk.exec.GuessExecutor;
import org.embulk.exec.SamplingParserPlugin;
import org.embulk.exec.NoSampleException;

public class FileInputRunner
        implements InputPlugin
{
    private final FileInputPlugin fileInputPlugin;

    public FileInputRunner(FileInputPlugin fileInputPlugin)
    {
        this.fileInputPlugin = fileInputPlugin;
    }

    private interface RunnerTask extends Task
    {
        // TODO "type" needed?

        @Config("decoders")
        @ConfigDefault("[]")
        public List<ConfigSource> getDecoderConfigs();

        @Config("parser")
        public ConfigSource getParserConfig();

        public void setFileInputTaskSource(TaskSource v);
        public TaskSource getFileInputTaskSource();

        public void setDecoderTaskSources(List<TaskSource> v);
        public List<TaskSource> getDecoderTaskSources();

        public void setParserTaskSource(TaskSource v);
        public TaskSource getParserTaskSource();
    }

    protected List<DecoderPlugin> newDecoderPlugins(RunnerTask task)
    {
        return Decoders.newDecoderPlugins(Exec.session(), task.getDecoderConfigs());
    }

    protected ParserPlugin newParserPlugin(RunnerTask task)
    {
        return Exec.newPlugin(ParserPlugin.class, task.getParserConfig().get(PluginType.class, "type"));
    }

    @Override
    public ConfigDiff transaction(ConfigSource config, final InputPlugin.Control control)
    {
        final RunnerTask task = config.loadConfig(RunnerTask.class);
        return fileInputPlugin.transaction(config, new RunnerControl(task, control));
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            Schema schema, int taskCount,
            InputPlugin.Control control)
    {
        final RunnerTask task = taskSource.loadTask(RunnerTask.class);
        return fileInputPlugin.resume(task.getFileInputTaskSource(), taskCount, new RunnerControl(task, control));
    }

    @Override
    public ConfigDiff guess(ConfigSource config)
    {
        Buffer sample = SamplingParserPlugin.runFileInputSampling(this, config);
        if (sample.limit() == 0) {
            throw new NoSampleException("Can't get sample data because the first input file is empty");
        }

        GuessExecutor guessExecutor = Exec.getInjector().getInstance(GuessExecutor.class);
        return guessExecutor.guessParserConfig(sample, config, Exec.session().getExecConfig());
    }

    private class RunnerControl
            implements FileInputPlugin.Control
    {
        private final RunnerTask task;
        private final List<DecoderPlugin> decoderPlugins;
        private final ParserPlugin parserPlugin;
        private final InputPlugin.Control nextControl;

        public RunnerControl(RunnerTask task, InputPlugin.Control nextControl)
        {
            this.task = task;
            // create plugins earlier than run() to throw exceptions early
            this.decoderPlugins = newDecoderPlugins(task);
            this.parserPlugin = newParserPlugin(task);
            this.nextControl = nextControl;
        }

        @Override
        public List<CommitReport> run(final TaskSource fileInputTaskSource, final int taskCount)
        {
            final List<CommitReport> commitReports = new ArrayList<CommitReport>();
            Decoders.transaction(decoderPlugins, task.getDecoderConfigs(), new Decoders.Control() {
                public void run(final List<TaskSource> decoderTaskSources)
                {
                    parserPlugin.transaction(task.getParserConfig(), new ParserPlugin.Control() {
                        public void run(final TaskSource parserTaskSource, final Schema schema)
                        {
                            task.setFileInputTaskSource(fileInputTaskSource);
                            task.setDecoderTaskSources(decoderTaskSources);
                            task.setParserTaskSource(parserTaskSource);
                            commitReports.addAll(nextControl.run(task.dump(), schema, taskCount));
                        }
                    });
                }
            });
            return commitReports;
        }
    }

    public void cleanup(TaskSource taskSource,
            Schema schema, int taskCount,
            List<CommitReport> successCommitReports)
    {
        fileInputPlugin.cleanup(taskSource, taskCount, successCommitReports);
    }

    @Override
    public CommitReport run(TaskSource taskSource, Schema schema, int taskIndex,
            PageOutput output)
    {
        final RunnerTask task = taskSource.loadTask(RunnerTask.class);
        List<DecoderPlugin> decoderPlugins = newDecoderPlugins(task);
        ParserPlugin parserPlugin = newParserPlugin(task);

        TransactionalFileInput tran = fileInputPlugin.open(task.getFileInputTaskSource(), taskIndex);
        FileInput fileInput = tran;
        try {
            fileInput = Decoders.open(decoderPlugins, task.getDecoderTaskSources(), fileInput);
            parserPlugin.run(task.getParserTaskSource(), schema, fileInput, output);

            CommitReport report = tran.commit();  // TODO check output.finish() is called. wrap
            tran = null;
            return report;
        } finally {
            try {
                if (tran != null) {
                    tran.abort();
                }
            } finally {
                fileInput.close();
            }
        }
    }
}
