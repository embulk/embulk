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
import org.embulk.config.CommitReport;
import org.embulk.plugin.PluginType;
import org.embulk.spi.Schema;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecSession;
import org.embulk.spi.ExecAction;

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
            return Exec.doWith(exec.copyForPreview(), new ExecAction<PreviewResult>() {
                public PreviewResult run()
                {
                    return doPreview(config);
                }
            });
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    protected InputPlugin newInputPlugin(PreviewTask task)
    {
        return Exec.newPlugin(InputPlugin.class, task.getInputConfig().get(PluginType.class, "type"));
    }

    private PreviewResult doPreview(ConfigSource config)
    {
        final PreviewTask task = config.loadConfig(PreviewTask.class);
        InputPlugin input = newInputPlugin(task);

        try {
            input.transaction(task.getInputConfig(), new InputPlugin.Control() {
                public List<CommitReport> run(TaskSource taskSource, Schema schema, int processorCount)
                {
                    InputPlugin input = newInputPlugin(task);
                    try (SamplingPageOutput out = new SamplingPageOutput(task.getSampleRows(), schema)) {
                        input.run(taskSource, schema, 0, out);
                    }
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
            for (Page page : pages) {
                page.release();
            }
        }
    }
}
