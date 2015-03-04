package org.embulk.exec;

import java.util.List;
import java.util.Arrays;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.common.base.Throwables;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.CommitReport;
import org.embulk.plugin.PluginType;
import org.embulk.spi.Schema;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecSession;
import org.embulk.spi.ExecAction;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.ProcessTask;
import org.embulk.spi.ProcessState;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.util.Filters;
import org.slf4j.Logger;

public class BulkLoader
{
    private final Injector injector;

    public interface BulkLoaderTask
            extends Task
    {
        @Config("exec")
        @ConfigDefault("{}")
        public ConfigSource getExecConfig();

        @Config("in")
        public ConfigSource getInputConfig();

        @Config("filters")
        @ConfigDefault("[]")
        public List<ConfigSource> getFilterConfigs();

        @Config("out")
        public ConfigSource getOutputConfig();

        public TaskSource getOutputTask();
        public void setOutputTask(TaskSource taskSource);
    }

    @Inject
    public BulkLoader(Injector injector,
            @ForSystemConfig ConfigSource systemConfig)
    {
        this.injector = injector;
    }

    private static class LoaderState
            implements ProcessState
    {
        private final Logger logger;

        private PluginType inputPluginType;
        private PluginType outputPluginType;
        private List<PluginType> filterPluginTypes;
        private int taskCount;

        private volatile TaskSource inputTaskSource;
        private volatile TaskSource outputTaskSource;
        private volatile List<TaskSource> filterTaskSources;
        private volatile List<Schema> schemas;

        private volatile boolean[] started;
        private volatile boolean[] finished;

        private volatile CommitReport[] inputCommitReports;
        private volatile CommitReport[] outputCommitReports;
        private volatile ConfigDiff inputConfigDiff;
        private volatile ConfigDiff outputConfigDiff;
        private volatile Throwable[] exceptions;

        public LoaderState(Logger logger)
        {
            this.logger = logger;
        }

        public Logger getLogger()
        {
            return logger;
        }

        public void setPluginTypes(BulkLoaderTask task)
        {
            this.inputPluginType = task.getInputConfig().get(PluginType.class, "type");
            this.outputPluginType = task.getOutputConfig().get(PluginType.class, "type");
            this.filterPluginTypes = Filters.getPluginTypes(task.getFilterConfigs());
        }

        public void initialize(int count)
        {
            this.started = new boolean[count];
            this.finished = new boolean[count];
            this.exceptions = new Throwable[count];
            this.inputCommitReports = new CommitReport[count];
            this.outputCommitReports = new CommitReport[count];
            this.taskCount = count;
        }

        public void setSchemas(List<Schema> schemas)
        {
            this.schemas = schemas;
        }

        public void setInputTaskSource(TaskSource inputTaskSource)
        {
            this.inputTaskSource = inputTaskSource;
        }

        public void setOutputTaskSource(TaskSource outputTaskSource)
        {
            this.outputTaskSource = outputTaskSource;
        }

        public void setFilterTaskSources(List<TaskSource> filterTaskSources)
        {
            this.filterTaskSources = filterTaskSources;
        }

        public ProcessTask buildProcessTask()
        {
            return new ProcessTask(
                    inputPluginType, outputPluginType, filterPluginTypes,
                    inputTaskSource, outputTaskSource, filterTaskSources,
                    schemas, Exec.newTaskSource());
        }

        @Override
        public void start(int taskIndex)
        {
            started[taskIndex] = true;
        }

        @Override
        public void finish(int taskIndex)
        {
            finished[taskIndex] = true;
        }

        @Override
        public boolean isStarted(int taskIndex)
        {
            return started[taskIndex];
        }

        @Override
        public boolean isFinished(int taskIndex)
        {
            return finished[taskIndex];
        }

        @Override
        public void setInputCommitReport(int taskIndex, CommitReport inputCommitReport)
        {
            if (inputCommitReport == null) {
                inputCommitReport = Exec.newCommitReport();
            }
            this.inputCommitReports[taskIndex] = inputCommitReport;
        }

        @Override
        public void setOutputCommitReport(int taskIndex, CommitReport outputCommitReport)
        {
            if (outputCommitReport == null) {
                outputCommitReport = Exec.newCommitReport();
            }
            this.outputCommitReports[taskIndex] = outputCommitReport;
        }

        @Override
        public boolean isOutputCommitted(int taskIndex)
        {
            return outputCommitReports[taskIndex] != null;
        }

        @Override
        public void setException(int taskIndex, Throwable exception)
        {
            this.exceptions[taskIndex] = exception;
        }

        @Override
        public boolean isExceptionSet(int taskIndex)
        {
            return this.exceptions[taskIndex] != null;
        }

        public boolean isAllCommitted()
        {
            if (taskCount <= 0) {
                // not initialized
                return false;
            }
            for (int i=0; i < taskCount; i++) {
                if (!isOutputCommitted(i)) {
                    return false;
                }
            }
            return true;
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

        public void setOutputConfigDiff(ConfigDiff outputConfigDiff)
        {
            if (outputConfigDiff == null) {
                outputConfigDiff = Exec.newConfigDiff();
            }
            this.outputConfigDiff = outputConfigDiff;
        }

        public void setInputConfigDiff(ConfigDiff inputConfigDiff)
        {
            if (inputConfigDiff == null) {
                inputConfigDiff = Exec.newConfigDiff();
            }
            this.inputConfigDiff = inputConfigDiff;
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

        public ExecutionResult buildExecuteResult()
        {
            return buildExecuteResultWithWarningException(null);
        }

        public ExecutionResult buildExecuteResultWithWarningException(Throwable ex)
        {
            ConfigDiff configDiff = Exec.newConfigDiff();
            if (inputConfigDiff != null) {
                configDiff.getNestedOrSetEmpty("in").merge(inputConfigDiff);
            }
            if (outputConfigDiff != null) {
                configDiff.getNestedOrSetEmpty("out").merge(outputConfigDiff);
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

            return new ExecutionResult(configDiff, ignoredExceptions.build());
        }

        public ResumeState buildResumeState(ExecSession exec)
        {
            return new ResumeState(
                    exec.getSessionConfigSource(),
                    inputTaskSource, outputTaskSource,
                    first(schemas), last(schemas),
                    Arrays.asList(inputCommitReports), Arrays.asList(outputCommitReports));
        }

        public PartialExecutionException buildPartialExecuteException(Throwable cause, ExecSession exec)
        {
            return new PartialExecutionException(cause, buildResumeState(exec));
        }
    }

    protected ExecutorPlugin newExecutorPlugin(BulkLoaderTask task)
    {
        // TODO
        return injector.getInstance(LocalExecutorPlugin.class);
    }

    protected InputPlugin newInputPlugin(BulkLoaderTask task)
    {
        return Exec.newPlugin(InputPlugin.class, task.getInputConfig().get(PluginType.class, "type"));
    }

    protected List<FilterPlugin> newFilterPlugins(BulkLoaderTask task)
    {
        return Filters.newFilterPlugins(Exec.session(),
                Filters.getPluginTypes(task.getFilterConfigs()));
    }

    protected OutputPlugin newOutputPlugin(BulkLoaderTask task)
    {
        return Exec.newPlugin(OutputPlugin.class, task.getOutputConfig().get(PluginType.class, "type"));
    }

    public ExecutionResult run(ExecSession exec, final ConfigSource config)
    {
        try {
            return Exec.doWith(exec, new ExecAction<ExecutionResult>() {
                public ExecutionResult run()
                {
                    try (SetCurrentThreadName dontCare = new SetCurrentThreadName("transaction")) {
                        return doRun(config);
                    }
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
                    try (SetCurrentThreadName dontCare = new SetCurrentThreadName("resume")) {
                        return doResume(config, resume);
                    }
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
                    try (SetCurrentThreadName dontCare = new SetCurrentThreadName("cleanup")) {
                        doCleanup(config, resume);
                        return null;
                    }
                }
            });
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public void doCleanup(ConfigSource config, ResumeState resume)
    {
        BulkLoaderTask task = config.loadConfig(BulkLoaderTask.class);
        InputPlugin inputPlugin = newInputPlugin(task);
        OutputPlugin outputPlugin = newOutputPlugin(task);

        List<CommitReport> successInputCommitReports = ImmutableList.copyOf(
                Iterables.filter(resume.getInputCommitReports(), Predicates.notNull()));
        List<CommitReport> successOutputCommitReports = ImmutableList.copyOf(
                Iterables.filter(resume.getOutputCommitReports(), Predicates.notNull()));

        inputPlugin.cleanup(resume.getInputTaskSource(), resume.getInputSchema(),
                resume.getInputCommitReports().size(), successInputCommitReports);

        outputPlugin.cleanup(resume.getOutputTaskSource(), resume.getOutputSchema(),
                resume.getOutputCommitReports().size(), successOutputCommitReports);
    }

    private ExecutionResult doRun(ConfigSource config)
    {
        final BulkLoaderTask task = config.loadConfig(BulkLoaderTask.class);

        final ExecutorPlugin exec = newExecutorPlugin(task);
        final InputPlugin inputPlugin = newInputPlugin(task);
        final List<FilterPlugin> filterPlugins = newFilterPlugins(task);
        final OutputPlugin outputPlugin = newOutputPlugin(task);

        final LoaderState state = new LoaderState(Exec.getLogger(BulkLoader.class));
        state.setPluginTypes(task);
        try {
            exec.transaction(task.getExecConfig(), new ExecutorPlugin.Control() {
                public void transaction(final ExecutorPlugin.Executor executor)
                {
                    ConfigDiff inputConfigDiff = inputPlugin.transaction(task.getInputConfig(),
                    new InputPlugin.Control() {
                        public List<CommitReport> run(final TaskSource inputTask, final Schema inputSchema, final int taskCount)
                        {
                            state.initialize(taskCount);
                            state.setInputTaskSource(inputTask);
                            Filters.transaction(filterPlugins, task.getFilterConfigs(), inputSchema, new Filters.Control() {
                                public void run(final List<TaskSource> filterTasks, final List<Schema> schemas)
                                {
                                    state.setSchemas(schemas);
                                    state.setFilterTaskSources(filterTasks);
                                    ConfigDiff outputConfigDiff = outputPlugin.transaction(task.getOutputConfig(), last(schemas), taskCount, new OutputPlugin.Control() {
                                        public List<CommitReport> run(final TaskSource outputTask)
                                        {
                                            state.setOutputTaskSource(outputTask);

                                            execute(task, executor, taskCount, state);

                                            return state.getOutputCommitReports();
                                        }
                                    });
                                    state.setOutputConfigDiff(outputConfigDiff);
                                }
                            });
                            return state.getInputCommitReports();
                        }
                    });
                    state.setInputConfigDiff(inputConfigDiff);
                }
            });

            cleanupCommittedTransaction(config, state);

            return state.buildExecuteResult();

        } catch (Throwable ex) {
            if (state.isAllCommitted()) {
                // ignore the exception
                return state.buildExecuteResultWithWarningException(ex);
            }
            if (!state.isAnyStarted()) {
                throw ex;
            }
            throw state.buildPartialExecuteException(ex, Exec.session());
        }
    }

    private ExecutionResult doResume(ConfigSource config, final ResumeState resume)
    {
        final BulkLoaderTask task = config.loadConfig(BulkLoaderTask.class);

        final ExecutorPlugin exec = newExecutorPlugin(task);
        final InputPlugin inputPlugin = newInputPlugin(task);
        final List<FilterPlugin> filterPlugins = newFilterPlugins(task);
        final OutputPlugin outputPlugin = newOutputPlugin(task);

        final LoaderState state = new LoaderState(Exec.getLogger(BulkLoader.class));
        state.setPluginTypes(task);
        try {
            exec.transaction(task.getExecConfig(), new ExecutorPlugin.Control() {
                public void transaction(final ExecutorPlugin.Executor executor)
                {
                    ConfigDiff inputConfigDiff = inputPlugin.resume(resume.getInputTaskSource(), resume.getInputSchema(), resume.getInputCommitReports().size(), new InputPlugin.Control() {
                        public List<CommitReport> run(final TaskSource inputTask, final Schema inputSchema, final int taskCount)
                        {
                            // TODO validate inputTask?
                            // TODO validate inputSchema
                            // TODO validate taskCount
                            state.initialize(taskCount);
                            state.setInputTaskSource(inputTask);
                            Filters.transaction(filterPlugins, task.getFilterConfigs(), inputSchema, new Filters.Control() {
                                public void run(final List<TaskSource> filterTasks, final List<Schema> schemas)
                                {
                                    state.setSchemas(schemas);
                                    state.setFilterTaskSources(filterTasks);
                                    ConfigDiff outputConfigDiff = outputPlugin.resume(resume.getOutputTaskSource(), last(schemas), taskCount, new OutputPlugin.Control() {
                                        public List<CommitReport> run(final TaskSource outputTask)
                                        {
                                            // TODO validate outputTask?
                                            state.setOutputTaskSource(outputTask);

                                            restoreResumedCommitReports(resume, taskCount, state);
                                            execute(task, executor, taskCount, state);

                                            return state.getOutputCommitReports();
                                        }
                                    });
                                    state.setOutputConfigDiff(outputConfigDiff);
                                }
                            });
                            return state.getInputCommitReports();
                        }
                    });
                    state.setInputConfigDiff(inputConfigDiff);
                }
            });

            cleanupCommittedTransaction(config, state);

            return state.buildExecuteResult();

        } catch (Throwable ex) {
            if (state.isAllCommitted()) {
                // ignore the exception
                return state.buildExecuteResultWithWarningException(ex);
            }
            if (!state.isAnyStarted()) {
                throw ex;
            }
            throw state.buildPartialExecuteException(ex, Exec.session());
        }
    }

    private static void restoreResumedCommitReports(ResumeState resume, int taskCount, LoaderState state)
    {
        for (int i=0; i < taskCount; i++) {
            if (resume.getOutputCommitReports().get(i) != null) {
                state.start(i);
                state.setInputCommitReport(i, resume.getInputCommitReports().get(i));
                state.setOutputCommitReport(i, resume.getOutputCommitReports().get(i));
                state.finish(i);
            }
        }
    }

    private void execute(BulkLoaderTask task, ExecutorPlugin.Executor executor,
            int taskCount, LoaderState state)
    {
        if (taskCount <= 0) {
            // TODO warning?
            return;
        }

        ProcessTask procTask = state.buildProcessTask();

        executor.execute(procTask, taskCount, state);

        if (!state.isAllCommitted()) {
            throw state.getRepresentativeException();
        }
    }

    private void cleanupCommittedTransaction(ConfigSource config, LoaderState state)
    {
        try {
            doCleanup(config, state.buildResumeState(Exec.session()));
        } catch (Exception ex) {
            state.getLogger().warn("Commit succeeded but cleanup failed. Ignoring this exception.", ex);  // TODO
        }
    }

    private static Schema first(List<Schema> schemas)
    {
        return schemas.get(0);
    }

    private static Schema last(List<Schema> schemas)
    {
        return schemas.get(schemas.size() - 1);
    }
}
