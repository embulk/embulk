package org.embulk.exec;

import java.util.List;
import java.util.ArrayList;
import javax.validation.constraints.NotNull;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.common.base.Throwables;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.plugin.PluginType;
import org.embulk.spi.Buffer;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.Schema;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecSession;
import org.embulk.spi.ExecAction;
import org.embulk.spi.util.Filters;

public class PreviewExecutor
{
    private final Injector injector;
    private final ConfigSource systemConfig;

    public interface PreviewTask
            extends Task
    {
        @Config("in")
        @NotNull
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

    @Inject
    public PreviewExecutor(Injector injector,
            @ForSystemConfig ConfigSource systemConfig)
    {
        this.injector = injector;
        this.systemConfig = systemConfig;
    }

    public PreviewResult preview(ExecSession exec, final ConfigSource config)
    {
        try {
            return Exec.doWith(exec.forPreview(), new ExecAction<PreviewResult>() {
                public PreviewResult run()
                {
                    try (SetCurrentThreadName dontCare = new SetCurrentThreadName("preview")) {
                        return doPreview(config);
                    }
                }
            });
        } catch (Exception ex) {
            throw Throwables.propagate(ex.getCause());
        }
    }

    protected InputPlugin newInputPlugin(PreviewTask task)
    {
        return Exec.newPlugin(InputPlugin.class, task.getInputConfig().get(PluginType.class, "type"));
    }

    protected List<FilterPlugin> newFilterPlugins(PreviewTask task)
    {
        return Filters.newFilterPluginsFromConfigSources(Exec.session(), task.getFilterConfigs());
    }

    private PreviewResult doPreview(ConfigSource config)
    {
        PreviewTask task = config.loadConfig(PreviewTask.class);
        InputPlugin inputPlugin = newInputPlugin(task);
        List<FilterPlugin> filterPlugins = newFilterPlugins(task);

        if (inputPlugin instanceof FileInputRunner) { // file input runner
            Buffer sample = SamplingParserPlugin.runFileInputSampling((FileInputRunner)inputPlugin, config.getNested("in"));
            FileInputRunner previewRunner = new FileInputRunner(new BufferFileInputPlugin(sample));
            return doPreview(task, previewRunner, filterPlugins);
        }
        else {
            return doPreview(task, inputPlugin, filterPlugins);
        }
    }

    private PreviewResult doPreview(final PreviewTask task, final InputPlugin input, final List<FilterPlugin> filterPlugins)
    {
        try {
            input.transaction(task.getInputConfig(), new InputPlugin.Control() {
                public List<TaskReport> run(final TaskSource inputTask, Schema inputSchema, final int taskCount)
                {
                    Filters.transaction(filterPlugins, task.getFilterConfigs(), inputSchema, new Filters.Control() {
                        public void run(final List<TaskSource> filterTasks, final List<Schema> filterSchemas)
                        {
                            Schema inputSchema = filterSchemas.get(0);
                            Schema outputSchema = filterSchemas.get(filterSchemas.size() - 1);

                            PageOutput out = new SamplingPageOutput(task.getSampleRows(), outputSchema);
                            try {
                                for (int taskIndex=0; taskIndex < taskCount; taskIndex++) {
                                    try {
                                        out = Filters.open(filterPlugins, filterTasks, filterSchemas, out);
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

    private static class SamplingPageOutput
            implements PageOutput
    {
        private final int sampleRows;
        private final Schema schema;
        private List<Page> pages;
        private int recordCount;

        public SamplingPageOutput(int sampleRows, Schema schema)
        {
            this.sampleRows = sampleRows;
            this.schema = schema;
            this.pages = new ArrayList<Page>();
        }

        public int getRecordCount()
        {
            return recordCount;
        }

        @Override
        public void add(Page page)
        {
            pages.add(page);
            recordCount += PageReader.getRecordCount(page);
            if (recordCount >= sampleRows) {
                finish();
            }
        }

        @Override
        public void finish()
        {
            if (recordCount == 0) {
                throw new NoSampleException("No input records to preview");
            }
            PreviewResult res = new PreviewResult(schema, pages);
            pages = null;
            throw new PreviewedNoticeError(res);
        }

        @Override
        public void close()
        {
            if (pages != null) {
                for (Page page : pages) {
                    page.release();
                }
                pages = null;
            }
        }
    }
}
