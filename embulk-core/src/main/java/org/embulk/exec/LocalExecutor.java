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
        @NotNull
        public ConfigSource getInputConfig();

        // TODO
        @Config("out")
        @NotNull
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

    protected InputPlugin newInputPlugin(ExecTask exec, ExecutorTask task)
    {
        return exec.newPlugin(InputPlugin.class, task.getInputConfig().get("type"));
    }

    protected OutputPlugin newOutputPlugin(ExecTask exec, ExecutorTask task)
    {
        return exec.newPlugin(OutputPlugin.class, task.getOutputConfig().get("type"));
    }

    public ExecuteResult run(ConfigSource config)
    {
        try {
            return doRun(config);
        } catch (Throwable ex) {
            throw PluginExecutors.propagePluginExceptions(ex);
        }
    }

    private ExecuteResult doRun(ConfigSource config)
    {
        final ExecTask exec = PluginExecutors.newExecTask(injector, config);
        final ExecutorTask task = exec.loadConfig(config, ExecutorTask.class);

        final InputPlugin in = newInputPlugin(exec, task);
        final OutputPlugin out = newOutputPlugin(exec, task);

        final TransactionContext tranContext = new TransactionContext();

        // TODO create and use ExecTaskBuilder to set default values

        NextConfig inputNextConfig = in.runInputTransaction(exec, task.getInputConfig(), new ExecControl() {
            public List<Report> run(final TaskSource inputTask)
            {
                NextConfig outputNextConfig = out.runOutputTransaction(exec, task.getOutputConfig(), new ExecControl() {
                    public List<Report> run(final TaskSource outputTask)
                    {
                        task.setInputTask(inputTask);
                        task.setOutputTask(outputTask);

                        exec.notice().debug("input: %s", task.getInputTask());
                        exec.notice().debug("output: %s", task.getOutputTask());

                        ProcessResult procResult = process(exec, exec.dumpTask(task), exec.getProcessorCount());
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

    private ProcessResult process(final ExecTask exec, final TaskSource taskSource, final int processorCount)
    {
        ProcessResult procResult = new ProcessResult(processorCount);

        List<PluginThread> processors = new ArrayList<>();

        try {
            for (int i=0; i < exec.getProcessorCount(); i++) {
                processors.add(startProcessor(exec, taskSource, procResult, i));
            }
        } finally {
            PluginThread.joinAndThrowNested(processors);
        }

        return procResult;
    }

    private PluginThread startProcessor(final ExecTask exec, final TaskSource taskSource,
            final ProcessResult procResult, final int processorIndex)
    {
        return exec.startPluginThread(new Runnable() {
            public void run()
            {
                final ExecutorTask task = exec.loadTask(taskSource, ExecutorTask.class);
                final InputPlugin in = newInputPlugin(exec, task);
                final OutputPlugin out = newOutputPlugin(exec, task);

                PluginThread thread = null;
                Throwable error = null;
                try (final PageChannel channel = exec.newPageChannel()) {
                    thread = exec.startPluginThread(new Runnable() {
                        public void run()
                        {
                            try {
                                // TODO return Report
                                Report report = out.runOutput(exec, task.getOutputTask(),
                                    processorIndex, channel.getInput());
                                procResult.setOutputReport(processorIndex, report);
                            } catch (Throwable ex) {
                                procResult.setOutputReport(processorIndex, new FailedReport(ex));
                                throw ex;  // TODO error handling to propagate exceptions to runInputTransaction should be at the end of process() once multi-threaded
                            } finally {
                                channel.completeConsumer();
                            }
                        }
                    });

                    try {
                        Report report = in.runInput(exec, task.getInputTask(),
                                processorIndex, channel.getOutput());
                        channel.completeProducer();
                        thread.join();
                        channel.join();  // throws if channel is fully consumed
                        procResult.setInputReport(processorIndex, report);
                    } catch (Throwable ex) {
                        procResult.setInputReport(processorIndex, new FailedReport(ex));
                        throw ex;  // TODO error handling to propagate exceptions to runInputTransactioat the end of process() once multi-threaded
                    }

                    procResult.addNotices(exec.notice());
                } catch (Throwable ex) {
                    error = ex;
                    procResult.addNotices(exec.notice());
                } finally {
                    PluginThread.joinAndThrowNested(thread, error);
                }
            }
        });
    }
}
