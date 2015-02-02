package org.embulk.exec;

import java.util.List;
import java.util.Arrays;
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
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
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
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.PageOutput;
import org.embulk.spi.TransactionalPageOutput;
import org.embulk.spi.util.Filters;
import org.slf4j.Logger;

public class LocalExecutor
{
    private final Injector injector;
    private final ConfigSource systemConfig;
    private final int maxThreads;
    private final ExecutorService executor;

    public interface ExecutorTask
            extends Task
    {
        @Config("in")
        public ConfigSource getInputConfig();

        @Config("filters")
        @ConfigDefault("[]")
        public List<ConfigSource> getFilterConfigs();

        @Config("out")
        public ConfigSource getOutputConfig();

        public TaskSource getInputTask();
        public void setInputTask(TaskSource taskSource);

        public List<TaskSource> getFilterTasks();
        public void setFilterTasks(List<TaskSource> taskSources);

        public TaskSource getOutputTask();
        public void setOutputTask(TaskSource taskSource);
    }

    @Inject
    public LocalExecutor(Injector injector,
            @ForSystemConfig ConfigSource systemConfig)
    {
        this.injector = injector;
        this.systemConfig = systemConfig;

        int defaultMaxThreads = Runtime.getRuntime().availableProcessors() * 2;
        this.maxThreads = systemConfig.get(Integer.class, "max_threads", defaultMaxThreads);
        this.executor = Executors.newFixedThreadPool(maxThreads,
                new ThreadFactoryBuilder()
                        .setNameFormat("embulk-executor-%d")
                        .setDaemon(true)
                        .build());
    }

    private static class ProcessState
    {
        private final Logger logger;
        private volatile boolean[] started;
        private volatile boolean[] finished;
        private volatile Schema inputSchema;
        private volatile Schema outputSchema;
        private volatile Throwable[] exceptions;
        private volatile CommitReport[] inputCommitReports;
        private volatile CommitReport[] outputCommitReports;
        private volatile NextConfig inputNextConfig;
        private volatile NextConfig outputNextConfig;
        private int processorCount;

        public ProcessState(Logger logger)
        {
            this.logger = logger;
        }

        public Logger getLogger()
        {
            return logger;
        }

        public void initialize(int count)
        {
            this.started = new boolean[count];
            this.finished = new boolean[count];
            this.exceptions = new Throwable[count];
            this.inputCommitReports = new CommitReport[count];
            this.outputCommitReports = new CommitReport[count];
            this.processorCount = count;
        }

        public void setInputSchema(Schema inputSchema)
        {
            this.inputSchema = inputSchema;
        }

        public void setOutputSchema(Schema outputSchema)
        {
            this.outputSchema = outputSchema;
        }

        public Schema getInputSchema()
        {
            return inputSchema;
        }

        public Schema getOutputSchema()
        {
            return outputSchema;
        }

        public boolean isAnyStarted()
        {
            if (started == null) {
                return false;
            }
            for (boolean b : started) {
                if (b) { return true; }
            }
            return false;
        }

        public void start(int i)
        {
            started[i] = true;
        }

        public void finish(int i)
        {
            finished[i] = true;
        }

        public int getProcessrCount()
        {
            return processorCount;
        }

        public int getStartedCount()
        {
            int count = 0;
            for (int i=0; i < started.length; i++) {
                if (started[i]) { count++; }
            }
            return count;
        }

        public int getFinishedCount()
        {
            int count = 0;
            for (int i=0; i < finished.length; i++) {
                if (finished[i]) { count++; }
            }
            return count;
        }

        public void setInputCommitReport(int i, CommitReport inputCommitReport)
        {
            if (inputCommitReport == null) {
                inputCommitReport = Exec.newCommitReport();
            }
            this.inputCommitReports[i] = inputCommitReport;
        }

        public void setOutputCommitReport(int i, CommitReport outputCommitReport)
        {
            if (outputCommitReport == null) {
                outputCommitReport = Exec.newCommitReport();
            }
            this.outputCommitReports[i] = outputCommitReport;
        }

        public boolean isOutputCommitted(int i)
        {
            return outputCommitReports[i] != null;
        }

        public void setException(int i, Throwable exception)
        {
            this.exceptions[i] = exception;
        }

        public boolean isAllCommitted()
        {
            if (processorCount <= 0) {
                // not initialized
                return false;
            }
            for (int i=0; i < processorCount; i++) {
                if (!isOutputCommitted(i)) {
                    return false;
                }
            }
            return true;
        }

        public boolean isAnyCommitted()
        {
            for (int i=0; i < processorCount; i++) {
                if (isOutputCommitted(i)) {
                    return true;
                }
            }
            return false;
        }

        public void setOutputNextConfig(NextConfig outputNextConfig)
        {
            if (outputNextConfig == null) {
                outputNextConfig = Exec.newNextConfig();
            }
            this.outputNextConfig = outputNextConfig;
        }

        public void setInputNextConfig(NextConfig inputNextConfig)
        {
            if (inputNextConfig == null) {
                inputNextConfig = Exec.newNextConfig();
            }
            this.inputNextConfig = inputNextConfig;
        }

        public List<CommitReport> getInputCommitReports()
        {
            return ImmutableList.copyOf(inputCommitReports);
        }

        public List<CommitReport> getOutputCommitReports()
        {
            return ImmutableList.copyOf(outputCommitReports);
        }

        public RuntimeException getRepresentativeException()
        {
            RuntimeException top = null;
            for (Throwable ex : exceptions) {
                if (ex != null) {
                    if (top != null) {
                        top.addSuppressed(ex);
                    } else {
                        if (ex instanceof RuntimeException) {
                            top = (RuntimeException) ex;
                        } else {
                            top = new RuntimeException(ex);
                        }
                    }
                }
            }
            if (top == null) {
                top = new RuntimeException("Some transactions are not committed");
            }
            return top;
        }

        public int getCommittedUnclosedCount()
        {
            int count = 0;
            for (int i=0; i < exceptions.length; i++) {
                if (exceptions[i] != null && isOutputCommitted(i)) {
                    count++;
                }
            }
            return count;
        }

        public ExecutionResult buildExecuteResult()
        {
            return buildExecuteResultWithWarningException(null);
        }

        public ExecutionResult buildExecuteResultWithWarningException(Throwable ex)
        {
            NextConfig nextConfig = Exec.newNextConfig();
            if (inputNextConfig != null) {
                nextConfig.getNestedOrSetEmpty("in").merge(inputNextConfig);
            }
            if (outputNextConfig != null) {
                nextConfig.getNestedOrSetEmpty("out").merge(outputNextConfig);
            }

            ImmutableList.Builder<Throwable> ignoredExceptions = ImmutableList.builder();
            for (Throwable e : exceptions) {
                if (e != null) {
                    ignoredExceptions.add(e);
                }
            }
            if (ex != null) {
                ignoredExceptions.add(ex);
            }

            return new ExecutionResult(nextConfig, ignoredExceptions.build());
        }

        public PartialExecutionException buildPartialExecuteException(Throwable cause,
                ExecutorTask task, ExecSession exec)
        {
            return new PartialExecutionException(cause, new ResumeState(
                        exec.getSessionConfigSource(),
                        task.getInputTask(), task.getOutputTask(),
                        inputSchema, outputSchema, processorCount,
                        Arrays.asList(inputCommitReports), Arrays.asList(outputCommitReports)));
        }
    }

    protected InputPlugin newInputPlugin(ExecutorTask task)
    {
        return Exec.newPlugin(InputPlugin.class, task.getInputConfig().get(PluginType.class, "type"));
    }

    protected List<FilterPlugin> newFilterPlugins(ExecutorTask task)
    {
        return Filters.newFilterPlugins(Exec.session(), task.getFilterConfigs());
    }

    protected OutputPlugin newOutputPlugin(ExecutorTask task)
    {
        return Exec.newPlugin(OutputPlugin.class, task.getOutputConfig().get(PluginType.class, "type"));
    }

    public ExecutionResult run(ExecSession exec, final ConfigSource config)
    {
        try {
            return Exec.doWith(exec, new ExecAction<ExecutionResult>() {
                public ExecutionResult run()
                {
                    return doRun(config);
                }
            });
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public ExecutionResult resume(final ConfigSource config, final ResumeState resume)
    {
        try {
            ExecSession exec = new ExecSession(injector, resume.getExecSessionConfigSource());
            return Exec.doWith(exec, new ExecAction<ExecutionResult>() {
                public ExecutionResult run()
                {
                    return doResume(config, resume);
                }
            });
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public void cleanup(final ConfigSource config, final ResumeState resume)
    {
        try {
            ExecSession exec = new ExecSession(injector, resume.getExecSessionConfigSource());
            Exec.doWith(exec, new ExecAction<Void>() {
                public Void run()
                {
                    doCleanup(config, resume);
                    return null;
                }
            });
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public void doCleanup(ConfigSource config, ResumeState resume)
    {
        ExecutorTask task = config.loadConfig(ExecutorTask.class);
        InputPlugin in = newInputPlugin(task);
        OutputPlugin out = newOutputPlugin(task);

        List<CommitReport> successInputCommitReports = ImmutableList.copyOf(
                Iterables.filter(resume.getInputCommitReports(), Predicates.notNull()));
        List<CommitReport> successOutputCommitReports = ImmutableList.copyOf(
                Iterables.filter(resume.getOutputCommitReports(), Predicates.notNull()));

        in.cleanup(resume.getInputTaskSource(), resume.getInputSchema(),
                resume.getProcessrCount(), successInputCommitReports);

        out.cleanup(resume.getOutputTaskSource(), resume.getOutputSchema(),
                resume.getProcessrCount(), successOutputCommitReports);
    }

    private ExecutionResult doRun(ConfigSource config)
    {
        final ExecutorTask task = config.loadConfig(ExecutorTask.class);

        final InputPlugin in = newInputPlugin(task);
        final List<FilterPlugin> filterPlugins = newFilterPlugins(task);
        final OutputPlugin out = newOutputPlugin(task);

        final ProcessState state = new ProcessState(Exec.getLogger(LocalExecutor.class));
        try {
            NextConfig inputNextConfig = in.transaction(task.getInputConfig(), new InputPlugin.Control() {
                public List<CommitReport> run(final TaskSource inputTask, final Schema inputSchema, final int processorCount)
                {
                    state.initialize(processorCount);
                    state.setInputSchema(inputSchema);
                    Filters.transaction(filterPlugins, task.getFilterConfigs(), inputSchema, new Filters.Control() {
                        public void run(final List<TaskSource> filterTasks, final List<Schema> filterSchemas)
                        {
                            Schema outputSchema = last(filterSchemas);
                            state.setOutputSchema(outputSchema);
                            NextConfig outputNextConfig = out.transaction(task.getOutputConfig(), outputSchema, processorCount, new OutputPlugin.Control() {
                                public List<CommitReport> run(final TaskSource outputTask)
                                {
                                    task.setInputTask(inputTask);
                                    task.setFilterTasks(filterTasks);
                                    task.setOutputTask(outputTask);

                                    process(task.dump(), filterSchemas, processorCount, state);
                                    if (!state.isAllCommitted()) {
                                        throw state.getRepresentativeException();
                                    }
                                    return state.getOutputCommitReports();
                                }
                            });
                            state.setOutputNextConfig(outputNextConfig);
                        }
                    });
                    return state.getInputCommitReports();
                }
            });
            state.setInputNextConfig(inputNextConfig);

            return state.buildExecuteResult();

        } catch (Throwable ex) {
            if (state.isAllCommitted()) {
                // ignore the exception
                return state.buildExecuteResultWithWarningException(ex);
            }
            if (!state.isAnyStarted()) {
                throw ex;
            }
            throw state.buildPartialExecuteException(ex, task, Exec.session());
        }
    }

    private ExecutionResult doResume(ConfigSource config, final ResumeState resume)
    {
        final ExecutorTask task = config.loadConfig(ExecutorTask.class);

        final InputPlugin in = newInputPlugin(task);
        final List<FilterPlugin> filterPlugins = newFilterPlugins(task);
        final OutputPlugin out = newOutputPlugin(task);

        final ProcessState state = new ProcessState(Exec.getLogger(LocalExecutor.class));
        try {
            NextConfig inputNextConfig = in.resume(resume.getInputTaskSource(), resume.getInputSchema(), resume.getProcessrCount(), new InputPlugin.Control() {
                public List<CommitReport> run(final TaskSource inputTask, final Schema inputSchema, final int processorCount)
                {
                    // TODO validate inputTask?
                    // TODO validate inputSchema
                    // TODO validate processorCount
                    state.initialize(processorCount);
                    Filters.transaction(filterPlugins, task.getFilterConfigs(), inputSchema, new Filters.Control() {
                        public void run(final List<TaskSource> filterTasks, final List<Schema> filterSchemas)
                        {
                            Schema outputSchema = last(filterSchemas);
                            state.setOutputSchema(outputSchema);
                            NextConfig outputNextConfig = out.resume(resume.getOutputTaskSource(), outputSchema, processorCount, new OutputPlugin.Control() {
                                public List<CommitReport> run(final TaskSource outputTask)
                                {
                                    // TODO validate outputTask?
                                    task.setInputTask(inputTask);
                                    task.setFilterTasks(filterTasks);
                                    task.setOutputTask(outputTask);

                                    for (int i=0; i < resume.getOutputCommitReports().size(); i++) {
                                        if (resume.getOutputCommitReports().get(i) != null) {
                                            state.start(i);
                                            state.setInputCommitReport(i, resume.getInputCommitReports().get(i));
                                            state.setOutputCommitReport(i, resume.getOutputCommitReports().get(i));
                                            state.finish(i);
                                        }
                                    }

                                    process(task.dump(), filterSchemas, processorCount, state);
                                    if (!state.isAllCommitted()) {
                                        throw state.getRepresentativeException();
                                    }
                                    return state.getOutputCommitReports();
                                }
                            });
                            state.setOutputNextConfig(outputNextConfig);
                        }
                    });
                    return state.getInputCommitReports();
                }
            });
            state.setInputNextConfig(inputNextConfig);

            return state.buildExecuteResult();

        } catch (Throwable ex) {
            if (state.isAllCommitted()) {
                // ignore the exception
                return state.buildExecuteResultWithWarningException(ex);
            }
            if (!state.isAnyStarted()) {
                throw ex;
            }
            throw state.buildPartialExecuteException(ex, task, Exec.session());
        }
    }

    private void process(TaskSource taskSource, List<Schema> filterSchemas, int processorCount,
            ProcessState state)
    {
        List<Future<Throwable>> futures = new ArrayList<>(processorCount);
        try {
            for (int i=0; i < processorCount; i++) {
                if (state.isOutputCommitted(i)) {
                    state.getLogger().warn("Skipped resumed task {}", i);
                    futures.add(null);  // resumed
                } else {
                    futures.add(startProcessor(taskSource, filterSchemas, i, state));
                }
            }
            showProgress(state);

            for (int i=0; i < processorCount; i++) {
                if (futures.get(i) == null) {
                    continue;
                }
                try {
                    state.setException(i, futures.get(i).get());
                } catch (ExecutionException ex) {
                    state.setException(i, ex.getCause());
                    //Throwables.propagate(ex.getCause());
                } catch (InterruptedException ex) {
                    state.setException(i, new ExecutionInterruptedException(ex));
                }
                showProgress(state);
            }
        } finally {
            for (Future<Throwable> future : futures) {
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                    // TODO join?
                }
            }
        }
    }

    private void showProgress(ProcessState state)
    {
        int total = state.getProcessrCount();
        int finished = state.getFinishedCount();
        int started = state.getStartedCount();
        state.getLogger().info(String.format("{done:%3d / %d, running: %d}", finished, total, started - finished));
    }

    private Future<Throwable> startProcessor(final TaskSource taskSource,
            final List<Schema> filterSchemas, final int index,
            final ProcessState state)
    {
        return executor.submit(new Callable<Throwable>() {
            public Throwable call()
            {
                final ExecutorTask task = taskSource.loadTask(ExecutorTask.class);
                final InputPlugin in = newInputPlugin(task);
                final List<FilterPlugin> filterPlugins = newFilterPlugins(task);
                final OutputPlugin out = newOutputPlugin(task);

                TransactionalPageOutput tran = out.open(task.getOutputTask(), last(filterSchemas), index);
                PageOutput closeThis = tran;
                state.start(index);
                try {
                    PageOutput filtered = closeThis = Filters.open(filterPlugins, task.getFilterTasks(), filterSchemas, tran);
                    state.setInputCommitReport(index, in.run(task.getInputTask(), first(filterSchemas), index, filtered));
                    state.setOutputCommitReport(index, tran.commit());  // TODO check output.finish() is called. wrap or abstract
                    return null;
                } finally {
                    try {
                        try {
                            if (!state.isOutputCommitted(index)) {
                                tran.abort();
                            }
                        } finally {
                            closeThis.close();
                        }
                    } finally {
                        state.finish(index);
                    }
                }
            }
        });
    }

    private static Schema first(List<Schema> filterSchemas)
    {
        return filterSchemas.get(0);
    }

    private static Schema last(List<Schema> filterSchemas)
    {
        return filterSchemas.get(filterSchemas.size() - 1);
    }
}
