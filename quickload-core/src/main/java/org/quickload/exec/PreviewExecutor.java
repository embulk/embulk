package org.quickload.exec;

import java.util.List;
import java.util.Iterator;
import com.google.common.collect.ImmutableList;
import javax.validation.constraints.NotNull;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.NextConfig;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.Report;
import org.quickload.channel.PageChannel;
import org.quickload.channel.PageInput;
import org.quickload.record.Schema;
import org.quickload.record.Column;
import org.quickload.record.Page;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.OutputPlugin;
import org.quickload.spi.PluginThread;
import org.quickload.spi.ProcTask;
import org.quickload.spi.ProcControl;

public class PreviewExecutor
{
    private final Injector injector;

    @Inject
    public PreviewExecutor(Injector injector)
    {
        this.injector = injector;
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
        ProcTask proc = new ProcTask(injector);
        return preview(proc, config);
    }

    protected InputPlugin newInputPlugin(ProcTask proc, PreviewTask task)
    {
        return proc.newPlugin(InputPlugin.class, task.getInputConfig().get("type"));
    }

    public PreviewResult preview(final ProcTask proc, ConfigSource config)
    {
        final PreviewTask task = proc.loadConfig(config, PreviewTask.class);
        final InputPlugin input = newInputPlugin(proc, task);

        try {
            input.runInputTransaction(proc, task.getInputConfig(), new ProcControl() {
                public List<Report> run(final TaskSource inputTaskSource)
                {
                    List<Page> pages;
                    try (final PageChannel channel = proc.newPageChannel()) {
                        proc.startPluginThread(new PluginThread() {
                            public void run()
                            {
                                try {
                                    input.runInput(proc, inputTaskSource, 0, channel.getOutput());
                                } finally {
                                    channel.completeProducer();
                                }
                            }
                        });

                        pages = getSample(channel.getInput(), task.getSampleRows());
                        channel.completeConsumer();
                        channel.join();
                    }
                    throw new PreviewedNoticeError(new PreviewResult(proc.getSchema(), pages));
                }
            });
            return new PreviewResult(proc.getSchema(), ImmutableList.<Page>of());
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
            throw new RuntimeException("empty");
        }
        return builder.build();
    }
}
