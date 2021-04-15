package org.embulk.exec;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import org.embulk.EmbulkSystemProperties;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.plugin.PluginType;
import org.embulk.spi.Buffer;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecAction;
import org.embulk.spi.ExecInternal;
import org.embulk.spi.ExecSessionInternal;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.util.FiltersInternal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewExecutor {
    public interface PreviewTask extends Task {
        @Config("exec")
        @ConfigDefault("{}")
        public ConfigSource getExecConfig();

        @Config("in")
        public ConfigSource getInputConfig();

        @Config("filters")
        @ConfigDefault("[]")
        public List<ConfigSource> getFilterConfigs();

        // TODO take preview_sample_rows from exec: config
        @Config("preview_sample_rows")
        @ConfigDefault("15")
        public int getSampleRows();

        public TaskSource getInputTask();

        public void setInputTask(TaskSource taskSource);
    }

    public interface PreviewExecutorTask extends Task {
        @Config("preview_sample_buffer_bytes")
        @ConfigDefault("null")
        public OptionalInt getSampleBufferBytes();
    }

    public PreviewExecutor(final EmbulkSystemProperties embulkSystemProperties) {
        this.embulkSystemProperties = embulkSystemProperties;
    }

    public PreviewResult preview(ExecSessionInternal exec, final ConfigSource config) {
        try {
            return ExecInternal.doWith(exec.forPreview(), new ExecAction<PreviewResult>() {
                    public PreviewResult run() {
                        try (SetCurrentThreadName dontCare = new SetCurrentThreadName("preview")) {
                            return doPreview(config);
                        }
                    }
                });
        } catch (Exception ex) {
            if (ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException) ex.getCause();
            }
            throw new RuntimeException(ex.getCause());
        }
    }

    protected InputPlugin newInputPlugin(PreviewTask task) {
        return ExecInternal.newPlugin(InputPlugin.class, task.getInputConfig().get(PluginType.class, "type"));
    }

    protected List<FilterPlugin> newFilterPlugins(PreviewTask task) {
        return FiltersInternal.newFilterPluginsFromConfigSources(ExecInternal.sessionInternal(), task.getFilterConfigs());
    }

    private PreviewResult doPreview(ConfigSource config) {
        final PreviewTask task = loadPreviewTask(config);
        InputPlugin inputPlugin = newInputPlugin(task);
        List<FilterPlugin> filterPlugins = newFilterPlugins(task);

        if (inputPlugin instanceof FileInputRunner) { // file input runner
            Buffer sample = SamplingParserPlugin.runFileInputSampling(
                    (FileInputRunner) inputPlugin,
                    config.getNested("in"),
                    createSampleBufferConfigFromExecConfig(task.getExecConfig(), this.embulkSystemProperties));
            FileInputRunner previewRunner = new FileInputRunner(new BufferFileInputPlugin(sample), this.embulkSystemProperties);
            return doPreview(task, previewRunner, filterPlugins);
        } else {
            return doPreview(task, inputPlugin, filterPlugins);
        }
    }

    private static ConfigSource createSampleBufferConfigFromExecConfig(
            final ConfigSource execConfig, final EmbulkSystemProperties embulkSystemProperties) {
        final PreviewExecutorTask execTask = loadPreviewExecutorTask(execConfig);
        final OptionalInt systemPreviewSampleBufferBytes =
                embulkSystemProperties.getPropertyAsOptionalInt("preview_sample_buffer_bytes");
        return Exec.newConfigSource().set(
                "sample_buffer_bytes",
                execTask.getSampleBufferBytes().orElse(systemPreviewSampleBufferBytes.orElse(DEAULT_SAMPLE_BUFFER_BYTES)));
    }

    @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
    private PreviewResult doPreview(final PreviewTask task, final InputPlugin input, final List<FilterPlugin> filterPlugins) {
        try {
            input.transaction(task.getInputConfig(), new InputPlugin.Control() {
                    public List<TaskReport> run(final TaskSource inputTask, Schema inputSchema, final int taskCount) {
                        FiltersInternal.transaction(filterPlugins, task.getFilterConfigs(), inputSchema, new FiltersInternal.Control() {
                                public void run(final List<TaskSource> filterTasks, final List<Schema> filterSchemas) {
                                    Schema inputSchema = filterSchemas.get(0);
                                    Schema outputSchema = filterSchemas.get(filterSchemas.size() - 1);

                                    PageOutput out = new SamplingPageOutput(task.getSampleRows(), outputSchema);
                                    try {
                                        for (int taskIndex = 0; taskIndex < taskCount; taskIndex++) {
                                            try {
                                                out = FiltersInternal.open(filterPlugins, filterTasks, filterSchemas, out);
                                                input.run(inputTask, inputSchema, taskIndex, out);
                                            } catch (NoSampleException ex) {
                                                if (taskIndex == taskCount - 1) {
                                                    throw ex;
                                                }
                                            }
                                        }
                                    } finally {
                                        out.close();
                                    }
                                }
                            });
                        // program never reaches here because SamplingPageOutput.finish throws an error.
                        throw new NoSampleException("No input records to preview");
                    }
                });
            throw new AssertionError("PreviewExecutor executor must throw PreviewedNoticeError");
        } catch (PreviewedNoticeError previewed) {
            return previewed.getPreviewResult();
        }
    }

    private static class SamplingPageOutput implements PageOutput {
        private final int sampleRows;
        private final Schema schema;
        private List<Page> pages;
        private int recordCount;
        private PreviewResult res;

        public SamplingPageOutput(int sampleRows, Schema schema) {
            this.sampleRows = sampleRows;
            this.schema = schema;
            this.pages = new ArrayList<Page>();
            this.res = null;
        }

        public int getRecordCount() {
            return recordCount;
        }

        @Override
        public void add(Page page) {
            pages.add(page);
            recordCount += PageReader.getRecordCount(page);
            if (recordCount >= sampleRows) {
                finish();
            }
        }

        @Override
        public void finish() {
            if (res != null) {
                logger.error("PreviewResult recreation will cause a bug. The plugin must call PageOutput#finish() only once.");
            }

            if (recordCount == 0) {
                throw new NoSampleException("No input records to preview");
            }
            res = new PreviewResult(schema, pages);
            pages = null;
            throw new PreviewedNoticeError(res);
        }

        @Override
        public void close() {
            if (pages != null) {
                for (Page page : pages) {
                    page.release();
                }
                pages = null;
            }
        }
    }

    @SuppressWarnings("deprecation") // https://github.com/embulk/embulk/issues/1301
    private static PreviewTask loadPreviewTask(final ConfigSource config) {
        final PreviewTask task = config.loadConfig(PreviewTask.class);
        if (task.getInputConfig() == null) {
            throw new ConfigException("'in' (InputConfig) must not be null.");
        }
        return task;
    }

    @SuppressWarnings("deprecation") // https://github.com/embulk/embulk/issues/1301
    private static PreviewExecutorTask loadPreviewExecutorTask(final ConfigSource config) {
        return config.loadConfig(PreviewExecutorTask.class);
    }

    private static final Logger logger = LoggerFactory.getLogger(PreviewExecutor.class);

    private static final int DEAULT_SAMPLE_BUFFER_BYTES = 32768;  // 32 * 1024

    private final EmbulkSystemProperties embulkSystemProperties;
}
