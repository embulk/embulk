package org.embulk.exec;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.plugin.PluginType;
import org.embulk.spi.Buffer;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecSession;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.util.Filters;
import org.slf4j.Logger;

public class PreviewExecutor {
    private final Injector injector;
    private final ConfigSource systemConfig;

    public interface PreviewTask extends Task {
        @Config("exec")
        @ConfigDefault("{}")
        ConfigSource getExecConfig();

        @Config("in")
        @NotNull
        ConfigSource getInputConfig();

        @Config("filters")
        @ConfigDefault("[]")
        List<ConfigSource> getFilterConfigs();

        // TODO take preview_sample_rows from exec: config
        @Config("preview_sample_rows")
        @ConfigDefault("15")
        int getSampleRows();

        TaskSource getInputTask();

        void setInputTask(TaskSource taskSource);
    }

    public interface PreviewExecutorTask extends Task {
        @Config("preview_sample_buffer_bytes")
        @ConfigDefault("32768") // 32 * 1024
        int getSampleBufferBytes();
    }

    @Inject
    public PreviewExecutor(Injector injector, @ForSystemConfig ConfigSource systemConfig) {
        this.injector = injector;
        this.systemConfig = systemConfig;
    }

    public PreviewResult preview(ExecSession exec, final ConfigSource config) {
        try {
            return Exec.doWith(exec.forPreview(), () -> {
                try (SetCurrentThreadName dontCare = new SetCurrentThreadName("preview")) {
                    return doPreview(config);
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
        return Exec.newPlugin(InputPlugin.class, task.getInputConfig().get(PluginType.class, "type"));
    }

    protected List<FilterPlugin> newFilterPlugins(PreviewTask task) {
        return Filters.newFilterPluginsFromConfigSources(Exec.session(), task.getFilterConfigs());
    }

    private PreviewResult doPreview(ConfigSource config) {
        PreviewTask task = config.loadConfig(PreviewTask.class);
        InputPlugin inputPlugin = newInputPlugin(task);
        List<FilterPlugin> filterPlugins = newFilterPlugins(task);

        if (inputPlugin instanceof FileInputRunner) { // file input runner
            Buffer sample = SamplingParserPlugin.runFileInputSampling(
                    (FileInputRunner) inputPlugin,
                    config.getNested("in"),
                    createSampleBufferConfigFromExecConfig(task.getExecConfig()));
            FileInputRunner previewRunner = new FileInputRunner(new BufferFileInputPlugin(sample));
            return doPreview(task, previewRunner, filterPlugins);
        } else {
            return doPreview(task, inputPlugin, filterPlugins);
        }
    }

    private static ConfigSource createSampleBufferConfigFromExecConfig(ConfigSource execConfig) {
        final PreviewExecutorTask execTask = execConfig.loadConfig(PreviewExecutorTask.class);
        return Exec.newConfigSource().set("sample_buffer_bytes", execTask.getSampleBufferBytes());
    }

    @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
    private PreviewResult doPreview(final PreviewTask task, final InputPlugin input, final List<FilterPlugin> filterPlugins) {
        try {
            input.transaction(task.getInputConfig(), (inputTask, inputSchema, taskCount) -> {
                Filters.transaction(filterPlugins, task.getFilterConfigs(), inputSchema, (filterTasks, filterSchemas) -> {
                    Schema inputSchema1 = filterSchemas.get(0);
                    Schema outputSchema = filterSchemas.get(filterSchemas.size() - 1);

                    PageOutput out = new SamplingPageOutput(task.getSampleRows(), outputSchema);
                    try {
                        for (int taskIndex = 0; taskIndex < taskCount; taskIndex++) {
                            try {
                                out = Filters.open(filterPlugins, filterTasks, filterSchemas, out);
                                input.run(inputTask, inputSchema1, taskIndex, out);
                            } catch (NoSampleException ex) {
                                if (taskIndex == taskCount - 1) {
                                    throw ex;
                                }
                            }
                        }
                    } finally {
                        out.close();
                    }
                });
                // program never reaches here because SamplingPageOutput.finish throws an error.
                throw new NoSampleException("No input records to preview");
            });
            throw new AssertionError("PreviewExecutor executor must throw PreviewedNoticeError");
        } catch (PreviewedNoticeError previewed) {
            return previewed.getPreviewResult();
        }
    }

    private static class SamplingPageOutput implements PageOutput {
        private final Logger log = Exec.getLogger(this.getClass());
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
                log.error("PreviewResult recreation will cause a bug. The plugin must call PageOutput#finish() only once.");
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
}
