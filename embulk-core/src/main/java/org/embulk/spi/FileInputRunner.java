package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.CommitReport;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.plugin.PluginType;
import org.embulk.type.Schema;

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
    public NextConfig transaction(ConfigSource config, final InputPlugin.Control control)
    {
        final RunnerTask task = config.loadConfig(RunnerTask.class);
        final List<DecoderPlugin> decoderPlugins = newDecoderPlugins(task);
        final ParserPlugin parserPlugin = newParserPlugin(task);

        return fileInputPlugin.transaction(config, new FileInputPlugin.Control() {
            public List<CommitReport> run(final TaskSource fileInputTaskSource, final int processorCount)
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
                                commitReports.addAll(control.run(task.dump(), schema, processorCount));
                            }
                        });
                    }
                });
                return commitReports;
            }
        });
    }

    @Override
    public CommitReport run(TaskSource taskSource, Schema schema, int processorIndex,
            PageOutput output)
    {
        final RunnerTask task = taskSource.loadTask(RunnerTask.class);
        List<DecoderPlugin> decoderPlugins = newDecoderPlugins(task);
        ParserPlugin parserPlugin = newParserPlugin(task);

        TransactionalFileInput tran = fileInputPlugin.open(taskSource, processorIndex);
        FileInput fileInput = tran;
        try {
            fileInput = Decoders.open(decoderPlugins, task.getDecoderTaskSources(), fileInput);
            parserPlugin.run(taskSource, schema, fileInput, output);

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
