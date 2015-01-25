package org.embulk.exec;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.NextConfig;
import org.embulk.config.CommitReport;
import org.embulk.plugin.PluginType;
import org.embulk.spi.Schema;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecSession;
import org.embulk.spi.ExecAction;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.TransactionalPageOutput;
import org.slf4j.Logger;

public class LocalExecutor
{
    private final Injector injector;
    private final ConfigSource systemConfig;
    private final ExecutorService executor;

    private Logger log;
    private final AtomicInteger runningTaskCount;
    private final AtomicInteger completedTaskCount;

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
        this.executor = newExecutorService(systemConfig);
        this.runningTaskCount = new AtomicInteger(0);
        this.completedTaskCount = new AtomicInteger(0);
    }

    private ExecutorService newExecutorService(ConfigSource systemConfig)
    {
        int defaultMaxThreads = Runtime.getRuntime().availableProcessors() * 2;
        int maxThreads = systemConfig.get(Integer.class, "max_threads",
                defaultMaxThreads);
        return Executors.newFixedThreadPool(maxThreads,
                new ThreadFactoryBuilder()
                        .setNameFormat("embulk-executor-%d")
                        .setDaemon(true)
                        .build());
    }

    private static class ExecuteResultBuilder
    {
        private NextConfig inputNextConfig;
        private NextConfig outputNextConfig;

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

        public ExecuteResult build()
        {
            if (inputNextConfig == null) {
                inputNextConfig = Exec.newNextConfig();
            }
            if (outputNextConfig == null) {
                outputNextConfig = Exec.newNextConfig();
            }
            NextConfig nextConfig = inputNextConfig.deepCopy().merge(outputNextConfig);
            return new ExecuteResult(nextConfig);
        }
    }

    private static class ProcessResult
    {
        private final CommitReport inputCommitReport;
        private final CommitReport outputCommitReport;

        public ProcessResult(CommitReport inputCommitReport, CommitReport outputCommitReport)
        {
            this.inputCommitReport = inputCommitReport;
            this.outputCommitReport = outputCommitReport;
        }

        public CommitReport getInputCommitReport()
        {
            return inputCommitReport;
        }

        public CommitReport getOutputCommitReport()
        {
            return outputCommitReport;
        }
    }

    protected InputPlugin newInputPlugin(ExecutorTask task)
    {
        return Exec.newPlugin(InputPlugin.class, task.getInputConfig().get(PluginType.class, "type"));
    }

    protected OutputPlugin newOutputPlugin(ExecutorTask task)
    {
        return Exec.newPlugin(OutputPlugin.class, task.getOutputConfig().get(PluginType.class, "type"));
    }

    public ExecuteResult run(ExecSession exec, final ConfigSource config)
    {
        log = exec.getLogger(LocalExecutor.class);
        try {
            return Exec.doWith(exec, new ExecAction<ExecuteResult>() {
                public ExecuteResult run()
                {
                    return doRun(config);
                }
            });
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    private ExecuteResult doRun(ConfigSource config)
    {
        final ExecutorTask task = config.loadConfig(ExecutorTask.class);

        final InputPlugin in = newInputPlugin(task);
        final OutputPlugin out = newOutputPlugin(task);

        final ExecuteResultBuilder execResult = new ExecuteResultBuilder();

        NextConfig inputNextConfig = in.transaction(task.getInputConfig(), new InputPlugin.Control() {
            public List<CommitReport> run(final TaskSource inputTask, final Schema schema, final int processorCount)
            {
                final ImmutableList.Builder<CommitReport> inputCommitReports = ImmutableList.builder();
                NextConfig outputNextConfig = out.transaction(task.getOutputConfig(), schema, processorCount, new OutputPlugin.Control() {
                    public List<CommitReport> run(final TaskSource outputTask)
                    {
                        final ImmutableList.Builder<CommitReport> outputCommitReports = ImmutableList.builder();
                        task.setInputTask(inputTask);
                        task.setOutputTask(outputTask);

                        //log.debug("input: %s", task.getInputTask());
                        //log.debug("output: %s", task.getOutputTask());

                        List<ProcessResult> results = process(task.dump(), schema, processorCount);
                        for (ProcessResult result : results) {
                            inputCommitReports.add(result.getInputCommitReport());
                            outputCommitReports.add(result.getOutputCommitReport());
                        }

                        return outputCommitReports.build();
                    }
                });
                execResult.setOutputNextConfig(outputNextConfig);
                return inputCommitReports.build();
            }
        });
        execResult.setInputNextConfig(inputNextConfig);

        return execResult.build();
    }

    private List<ProcessResult> process(TaskSource taskSource, Schema schema, int processorCount)
    {
        List<Future<ProcessResult>> futures = new ArrayList<>();
        List<ProcessResult> joined = new ArrayList<>();
        try {
            log.info("Start bulk processing");
            showProgress(processorCount);
            for (int i=0; i < processorCount; i++) {
                futures.add(startProcessor(taskSource, schema, i));
            }

            for (int i=0; i < processorCount; i++) {
                try {
                    joined.add(futures.get(i).get());
                    showProgress(processorCount);

                } catch (ExecutionException ex) {
                    throw Throwables.propagate(ex.getCause());
                } catch (InterruptedException ex) {
                    throw new ExecuteInterruptedException(ex);
                }
            }
            return joined;
        } finally {
            for (int i=joined.size(); i < futures.size(); i++) {
                futures.get(i).cancel(true);
                // TODO join?
            }
        }
    }

    private void showProgress(int total)
    {
        int running = runningTaskCount.get();
        int done = completedTaskCount.get();
        log.info(String.format("{done:%3d / %d, running: %d}", done, total, running));
    }

    private Future<ProcessResult> startProcessor(final TaskSource taskSource, final Schema schema, final int index)
    {
        return executor.submit(new Callable<ProcessResult>() {
            public ProcessResult call()
            {
                try {
                    runningTaskCount.getAndIncrement();
                    final ExecutorTask task = taskSource.loadTask(ExecutorTask.class);
                    final InputPlugin in = newInputPlugin(task);
                    final OutputPlugin out = newOutputPlugin(task);

                    TransactionalPageOutput tran = out.open(task.getOutputTask(), schema, index);
                    boolean committed = false;
                    try {
                        CommitReport inReport = in.run(task.getInputTask(), schema, index, tran);
                        CommitReport outReport = tran.commit();  // TODO check output.finish() is called. wrap or abstract
                        committed = true;
                        return new ProcessResult(inReport, outReport);
                    } finally {
                        if (!committed) {
                            tran.abort();
                        }
                        tran.close();
                    }
                } finally {
                    runningTaskCount.getAndDecrement();
                    completedTaskCount.getAndIncrement();
                }
            }
        });
    }
}
