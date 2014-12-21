package org.embulk.exec;

import java.util.List;
import java.util.ArrayList;
import javax.validation.constraints.NotNull;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
import org.embulk.config.FailedReport;
import org.embulk.spi.ExecControl;
import org.embulk.spi.ExecTask;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.PluginThread;
import org.embulk.spi.NoticeLogger;
import org.embulk.channel.PageChannel;

public class LocalExecutor
{
    private final Injector injector;
    private final ConfigSource systemConfig;

    public interface ExecutorTask
            extends Task
    {
        @Config("in")
        public ConfigSource getInputConfig();

        @Config("out")
        public ConfigSource getOutputConfig();

        public TaskSource getInputTask();
        public void setInputTask(TaskSource taskSource);

        public TaskSource getOutputTask();
        public void setOutputTask(TaskSource taskSource);
    }

    @Inject
    public LocalExecutor(Injector injector,
            @ForSystemConfig ConfigSource systemConfig)
    {
        this.injector = injector;
        this.systemConfig = systemConfig;
    }

    private static class TransactionContext
    {
        private List<Report> inputReports;
        private List<Report> outputReports;
        private NextConfig inputNextConfig;
        private NextConfig outputNextConfig;
        private List<NoticeLogger.Message> noticeMessages;
        private List<NoticeLogger.SkippedRecord> skippedRecords;

        public List<Report> getInputReports()
        {
            return inputReports;
        }

        public List<Report> getOutputReports()
        {
            return outputReports;
        }

        public void setInputReports(List<Report> inputReports)
        {
            this.inputReports = inputReports;
        }

        public void setOutputReports(List<Report> outputReports)
        {
            this.outputReports = outputReports;
        }

        public void setInputNextConfig(NextConfig inputNextConfig)
        {
            this.inputNextConfig = inputNextConfig;
        }

        public void setOutputNextConfig(NextConfig outputNextConfig)
        {
            this.outputNextConfig = outputNextConfig;
        }

        public NextConfig getInputNextConfig()
        {
            return inputNextConfig;
        }

        public NextConfig getOutputNextConfig()
        {
            return outputNextConfig;
        }

        public void setNoticeMessages(List<NoticeLogger.Message> noticeMessages)
        {
            this.noticeMessages = noticeMessages;
        }

        public void setSkippedRecords(List<NoticeLogger.SkippedRecord> skippedRecords)
        {
            this.skippedRecords = skippedRecords;
        }

        public List<NoticeLogger.Message> getNoticeMessages()
        {
            return noticeMessages;
        }

        public List<NoticeLogger.SkippedRecord> getSkippedRecords()
        {
            return skippedRecords;
        }
    }

    private static class ProcessResult
    {
        private final Report[] inputReports;
        private final Report[] outputReports;
        private final List<NoticeLogger.Message> noticeMessages;
        private final List<NoticeLogger.SkippedRecord> skippedRecords;

        public ProcessResult(int processorCount)
        {
            this.inputReports = new Report[processorCount];
            this.outputReports = new Report[processorCount];
            for (int i=0; i < processorCount; i++) {
                inputReports[i] = outputReports[i] = new Report();
            }
            this.noticeMessages = new ArrayList<NoticeLogger.Message>();
            this.skippedRecords = new ArrayList<NoticeLogger.SkippedRecord>();
        }

        public void setInputReport(int processorIndex, Report report)
        {
            inputReports[processorIndex] = report;
        }

        public void setOutputReport(int processorIndex, Report report)
        {
            outputReports[processorIndex] = report;
        }

        public void addNotices(NoticeLogger notice)
        {
            synchronized (noticeMessages) {
                notice.addAllMessagesTo(noticeMessages);
            }
            synchronized (skippedRecords) {
                notice.addAllSkippedRecordsTo(skippedRecords);
            }
        }

        public List<Report> getInputReports()
        {
            return ImmutableList.copyOf(inputReports);
        }

        public List<Report> getOutputReports()
        {
            return ImmutableList.copyOf(outputReports);
        }

        public List<NoticeLogger.Message> getNoticeMessages()
        {
            return noticeMessages;
        }

        public List<NoticeLogger.SkippedRecord> getSkippedRecords()
        {
            return skippedRecords;
        }
    }

    protected InputPlugin newInputPlugin(ExecutorTask task)
    {
        return Exec.newPlugin(InputPlugin.class, task.getInputConfig().get("type"));
    }

    protected OutputPlugin newOutputPlugin(ExecutorTask task)
    {
        return Exec.newPlugin(OutputPlugin.class, task.getOutputConfig().get("type"));
    }

    public ExecuteResult run(final ExecSession exec, final ConfigSource config)
    {
        Exec.doWith(exec, new ExecAction<ExecuteResult>() {
            public ExecuteResult run()
            {
                return doRun(config);
            }
        });
    }

    private ExecuteResult doRun(ConfigSource config)
    {
        final ExecutorTask task = Exec.loadConfig(config, ExecutorTask.class);

        final InputPlugin in = newInputPlugin(task);
        final OutputPlugin out = newOutputPlugin(task);

        final TransactionContext tranContext = new TransactionContext();

        // TODO create and use ExecTaskBuilder to set default values

        NextConfig inputNextConfig = in.transaction(task.getInputConfig(), new InputPlugin.Control() {
            public List<Report> run(final TaskSource inputTask, final Schema schema, final int processorCount)
            {
                NextConfig outputNextConfig = out.transaction(task.getOutputConfig(), schema, processorCount, new OutputPlugin.Control() {
                    public List<Report> run(final TaskSource outputTask)
                    {
                        task.setInputTask(inputTask);
                        task.setOutputTask(outputTask);

                        Exec.getLogger().debug("input: %s", task.getInputTask());
                        Exec.getLogger().debug("output: %s", task.getOutputTask());

                        ProcessResult procResult = process(Exec.dumpTask(task), processorCount);
                        tranContext.setOutputReports(procResult.getOutputReports());
                        tranContext.setInputReports(procResult.getInputReports());
                        tranContext.setNoticeMessages(procResult.getNoticeMessages());
                        tranContext.setSkippedRecords(procResult.getSkippedRecords());

                        return tranContext.getOutputReports();
                    }
                });
                tranContext.setOutputNextConfig(outputNextConfig);
                return tranContext.getInputReports();
            }
        });
        tranContext.setInputNextConfig(inputNextConfig);

        return new ExecuteResult(
                tranContext.getInputNextConfig().setAll(tranContext.getOutputNextConfig()),
                tranContext.getNoticeMessages(),
                tranContext.getSkippedRecords());
    }

    private ProcessResult process(TaskSource taskSource, Schema schema, int processorCount)
    {
        ProcessResult procResult = new ProcessResult(processorCount);

        List<PluginThread> processors = new ArrayList<>();

        try {
            for (int i=0; i < processorCount; i++) {
                processors.add(startProcessor(taskSource, procResult, Schema, i));
            }
        } finally {
            PluginThread.joinAndThrowNested(processors);
        }

        return procResult;
    }

    private PluginThread startProcessor(final TaskSource taskSource,
            final ProcessResult procResult, final int processorIndex)
    {
        return Exec.startPluginThread(new Runnable() {
            public void run()
            {
                final ExecutorTask task = Exec.loadTask(taskSource, ExecutorTask.class);
                final InputPlugin in = newInputPlugin(task);
                final OutputPlugin out = newOutputPlugin(task);

                Transactional tran = null;
                try {
                    try (PageOutput output = out.open(task.getOutputTask(), schema, processorIndex)) {
                        tran = (Transactional) output;
                        CommitReport report = in.run(task.getInputTask(), schema, processorIndex, output);
                        procResult.setInputReport(processorIndex, report);
                    }

                    CommitReport report = tran.commit();  // TODO check output.finish() is called. wrap or abstract
                    procResult.setOutputReport(processorIndex, report);
                    tran = null;
                } finally {
                    if (tran != null) {
                        tran.abort();
                    }
                }
                // TODO
                // procResult.addNotices(exec.notice());
            }
        });
    }
}
