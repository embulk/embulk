package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
import org.embulk.type.Schema;

public class FileInputRunner
        implements InputPlugin
{
    private interface RunnerTask extends Task
    {
        @Config("type")
        public JsonNode getType();

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

    protected FileInputPlugin newFileInputPlugin(RunnerTask task)
    {
        return Exec.newPlugin(FileInputPlugin.class, task.getType());
    }

    protected List<DecoderPlugin> newDecoderPlugins(RunnerTask task)
    {
        return Decoders.newDecoderPlugins(Exec.session(), task.getDecoderConfigs());
    }

    protected ParserPlugin newParserPlugin(RunnerTask task)
    {
        return Exec.newPlugin(ParserPlugin.class, task.getParserConfig().get("type"));
    }

    @Override
    public NextConfig transaction(ConfigSource config, final InputPlugin.Control control)
    {
        final RunnerTask task = config.loadConfig(RunnerTask.class);
        FileInputPlugin fileInputPlugin = newFileInputPlugin(task);
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
                                TaskSource taskSource = new TaskSource();
                                task.setFileInputTaskSource(fileInputTaskSource);
                                task.setDecoderTaskSources(decoderTaskSources);
                                task.setParserTaskSource(parserTaskSource);
                                commitReports.addAll(control.run(Exec.dumpTask(task), schema, processorCount));
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
            PageOutput output);
    {
        final RunnerTask task = taskSource.loadTask(RunnerTask.class);
        FileInputPlugin fileInputPlugin = newFileInputPlugin(task);
        List<DecoderPlugin> decoderPlugins = newDecoderPlugins(task);
        ParserPlugin parserPlugin = newParserPlugin(task);

        Transactional tran = null;
        try {
            try (FileInput fileInput = fileInputPlugin.open(taskSource, processorIndex)) {
                tran = (Transactional) fileInput;
                fileInput = Decoders.open(decoderPlugins, task.getDecoderTaskSources(), fileInput);

                parserPlugin.run(taskSource, schema, fileInput, output);
            }

            CommitReport report = tran.commit();  // TODO check output.finish() is called. wrap
            tran = null;
            return report;
        } finally {
            if (tran != null) {
                tran.abort();
            }
        }
    }
}
