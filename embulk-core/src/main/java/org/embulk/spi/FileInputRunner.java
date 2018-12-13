package org.embulk.spi;

import static org.embulk.exec.GuessExecutor.createSampleBufferConfigFromExecConfig;

import java.util.ArrayList;
import java.util.List;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.exec.ConfigurableGuessInputPlugin;
import org.embulk.exec.GuessExecutor;
import org.embulk.exec.SamplingParserPlugin;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.compat.PluginWrappers;
import org.embulk.spi.util.Decoders;

public class FileInputRunner implements InputPlugin, ConfigurableGuessInputPlugin {
    private final FileInputPlugin fileInputPlugin;

    public FileInputRunner(FileInputPlugin fileInputPlugin) {
        this.fileInputPlugin = fileInputPlugin;
    }

    private interface RunnerTask extends Task {
        // TODO "type" needed?

        @Config("decoders")
        @ConfigDefault("[]")
        List<ConfigSource> getDecoderConfigs();

        @Config("parser")
        ConfigSource getParserConfig();

        void setFileInputTaskSource(TaskSource v);

        TaskSource getFileInputTaskSource();

        void setDecoderTaskSources(List<TaskSource> v);

        List<TaskSource> getDecoderTaskSources();

        void setParserTaskSource(TaskSource v);

        TaskSource getParserTaskSource();
    }

    protected List<DecoderPlugin> newDecoderPlugins(RunnerTask task) {
        return Decoders.newDecoderPlugins(Exec.session(), task.getDecoderConfigs());
    }

    protected ParserPlugin newParserPlugin(RunnerTask task) {
        return Exec.newPlugin(ParserPlugin.class, task.getParserConfig().get(PluginType.class, "type"));
    }

    @Override
    public ConfigDiff transaction(ConfigSource config, final InputPlugin.Control control) {
        final RunnerTask task = config.loadConfig(RunnerTask.class);
        return fileInputPlugin.transaction(config, new RunnerControl(task, control));
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            Schema schema, int taskCount,
            InputPlugin.Control control) {
        final RunnerTask task = taskSource.loadTask(RunnerTask.class);
        return fileInputPlugin.resume(task.getFileInputTaskSource(), taskCount, new RunnerControl(task, control));
    }

    @Override
    public ConfigDiff guess(ConfigSource config) {
        return guess(Exec.newConfigSource(), config);
    }

    public ConfigDiff guess(ConfigSource execConfig, ConfigSource inputConfig) {
        final ConfigSource sampleBufferConfig = createSampleBufferConfigFromExecConfig(execConfig);
        final Buffer sample = SamplingParserPlugin.runFileInputSampling(this, inputConfig, sampleBufferConfig);
        // SamplingParserPlugin.runFileInputSampling throws NoSampleException if there're
        // no files or all files are smaller than minSampleSize (40 bytes).

        GuessExecutor guessExecutor = Exec.getInjector().getInstance(GuessExecutor.class);
        return guessExecutor.guessParserConfig(sample, inputConfig, execConfig);
    }

    private class RunnerControl implements FileInputPlugin.Control {
        private final RunnerTask task;
        private final List<DecoderPlugin> decoderPlugins;
        private final ParserPlugin parserPlugin;
        private final InputPlugin.Control nextControl;

        public RunnerControl(RunnerTask task, InputPlugin.Control nextControl) {
            this.task = task;
            // create plugins earlier than run() to throw exceptions early
            this.decoderPlugins = newDecoderPlugins(task);
            this.parserPlugin = newParserPlugin(task);
            this.nextControl = nextControl;
        }

        @Override
        public List<TaskReport> run(final TaskSource fileInputTaskSource, final int taskCount) {
            final List<TaskReport> taskReports = new ArrayList<TaskReport>();
            Decoders.transaction(
                    decoderPlugins,
                    task.getDecoderConfigs(),
                    decoderTaskSources -> parserPlugin.transaction(task.getParserConfig(), (parserTaskSource, schema) -> {
                        task.setFileInputTaskSource(fileInputTaskSource);
                        task.setDecoderTaskSources(decoderTaskSources);
                        task.setParserTaskSource(parserTaskSource);
                        taskReports.addAll(nextControl.run(task.dump(), schema, taskCount));
                    })
            );
            return taskReports;
        }
    }

    public void cleanup(TaskSource taskSource,
            Schema schema, int taskCount,
            List<TaskReport> successTaskReports) {
        fileInputPlugin.cleanup(taskSource, taskCount, successTaskReports);
    }

    @Override
    public TaskReport run(TaskSource taskSource, Schema schema, int taskIndex,
            PageOutput output) {
        final RunnerTask task = taskSource.loadTask(RunnerTask.class);
        List<DecoderPlugin> decoderPlugins = newDecoderPlugins(task);
        ParserPlugin parserPlugin = newParserPlugin(task);

        final TransactionalFileInput tran = PluginWrappers.transactionalFileInput(
                fileInputPlugin.open(task.getFileInputTaskSource(), taskIndex));
        try (CloseResource closer = new CloseResource(tran)) {
            try (AbortTransactionResource aborter = new AbortTransactionResource(tran)) {
                FileInput fileInput = Decoders.open(decoderPlugins, task.getDecoderTaskSources(), tran);
                closer.closeThis(fileInput);
                parserPlugin.run(task.getParserTaskSource(), schema, fileInput, output);

                TaskReport report = tran.commit();  // TODO check output.finish() is called. wrap
                aborter.dontAbort();
                return report;
            }
        }
    }

    public static TaskSource getFileInputTaskSource(TaskSource runnerTaskSource) {
        return runnerTaskSource.loadTask(RunnerTask.class).getFileInputTaskSource();
    }
}
