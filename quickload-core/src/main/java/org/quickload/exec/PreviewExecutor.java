package org.quickload.exec;

import java.util.List;
import com.google.common.collect.ImmutableList;
import javax.validation.constraints.NotNull;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.Report;
import org.quickload.channel.PageChannel;
import org.quickload.channel.PageInput;
import org.quickload.record.Page;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.PluginThread;
import org.quickload.spi.ExecTask;
import org.quickload.spi.ExecControl;

public class PreviewExecutor
{
    private final Injector injector;
    private final ConfigSource systemConfig;

    @Inject
    public PreviewExecutor(Injector injector,
            @ForSystemConfig ConfigSource systemConfig)
    {
        this.injector = injector;
        this.systemConfig = systemConfig;
    }

    public interface PreviewTask
            extends Task
    {
        @Config("in")
        @NotNull
        public ConfigSource getInputConfig();

        @Config("preview_sample_rows")
        @ConfigDefault("30")
        public int getSampleRows();

        public TaskSource getInputTask();
        public void setInputTask(TaskSource taskSource);
    }

    public PreviewResult run(ConfigSource config)
    {
        ExecTask exec = PluginExecutors.newExecTask(injector, config);
        return preview(exec, config);
    }

    protected InputPlugin newInputPlugin(ExecTask exec, PreviewTask task)
    {
        return exec.newPlugin(InputPlugin.class, task.getInputConfig().get("type"));
    }

    public PreviewResult preview(ExecTask exec, ConfigSource config)
    {
        try {
            return doPreview(exec, config);
        } catch (Throwable ex) {
            throw PluginExecutors.propagePluginExceptions(ex);
        }
    }

    private PreviewResult doPreview(final ExecTask exec, ConfigSource config)
    {
        final PreviewTask task = exec.loadConfig(config, PreviewTask.class);
        final InputPlugin input = newInputPlugin(exec, task);

        try {
            input.runInputTransaction(exec, task.getInputConfig(), new ExecControl() {
                public List<Report> run(final TaskSource inputTaskSource)
                {
                    List<Page> pages;
                    PluginThread thread = null;
                    try (final PageChannel channel = exec.newPageChannel()) {
                        thread = exec.startPluginThread(new Runnable() {
                            public void run()
                            {
                                try {
                                    input.runInput(exec, inputTaskSource, 0, channel.getOutput());
                                } finally {
                                    channel.completeProducer();
                                }
                            }
                        });

                        pages = getSample(channel.getInput(), task.getSampleRows());
                        channel.completeConsumer();
                        channel.join();
                    } finally {
                        // don't call joinAndThrow to ignore exceptions in InputPlugins
                        thread.join();
                    }
                    throw new PreviewedNoticeError(new PreviewResult(exec.getSchema(), pages,
                                exec.notice().getMessages(), exec.notice().getSkippedRecords()));
                }
            });
            return new PreviewResult(exec.getSchema(), ImmutableList.<Page>of(),
                    exec.notice().getMessages(), exec.notice().getSkippedRecords());
        } catch (PreviewedNoticeError previewed) {
            return previewed.getPreviewResult();
        }
    }

    public static List<Page> getSample(PageInput pageInput, int maxSampleRows)
    {
        int sampleRows = 0;
        ImmutableList.Builder<Page> builder = ImmutableList.builder();
        for (Page page : pageInput) {
            builder.add(page);
            sampleRows += page.getRecordCount();
            if (sampleRows >= maxSampleRows) {
                break;
            }
        }
        if (sampleRows == 0) {
            throw new NoSampleException("No input records to preview");
        }

        return builder.build();
    }
}
