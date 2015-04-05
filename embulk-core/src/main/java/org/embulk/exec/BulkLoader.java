package org.embulk.exec;

import java.util.List;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import com.google.common.base.Optional;
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
import org.embulk.config.ConfigException;
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
import org.embulk.spi.TaskState;
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

        private volatile TaskSource inputTaskSource;
        private volatile TaskSource outputTaskSource;
        private volatile List<TaskSource> filterTaskSources;
        private volatile List<Schema> schemas;
        private volatile Schema executorSchema;

        private volatile ConfigDiff inputConfigDiff;
        private volatile ConfigDiff outputConfigDiff;

        private volatile List<TaskState> inputTaskStates;
        private volatile List<TaskState> outputTaskStates;

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

        public void setSchemas(List<Schema> schemas)
        {
            this.schemas = schemas;
        }

        public void setExecutorSchema(Schema executorSchema)
        {
            this.executorSchema = executorSchema;
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
                    schemas, executorSchema, Exec.newTaskSource());
        }

        @Override
        public void initialize(int inputTaskCount, int outputTaskCount)
        {
            if (inputTaskStates != null || outputTaskStates != null) {
                // initialize is called twice if resume (by restoreResumedCommitReports and ExecutorPlugin.execute)
                if (inputTaskStates.size() != inputTaskCount || outputTaskStates.size() != outputTaskCount) {
                    throw new ConfigException(String.format(
                                "input task count and output task (%d and %d) must be same with the first execution (%d and %d) whenre resumed",
                                inputTaskCount, outputTaskCount, inputTaskStates.size(), outputTaskStates.size()));
                }
            } else {
                ImmutableList.Builder<TaskState> inputTaskStates = ImmutableList.builder();
                ImmutableList.Builder<TaskState> outputTaskStates = ImmutableList.builder();
                for (int i=0; i < inputTaskCount; i++) {
                    inputTaskStates.add(new TaskState());
                }
                for (int i=0; i < outputTaskCount; i++) {
                    outputTaskStates.add(new TaskState());
                }
                this.inputTaskStates = inputTaskStates.build();
                this.outputTaskStates = outputTaskStates.build();
            }
        }

        @Override
        public TaskState getInputTaskState(int inputTaskIndex)
        {
            return inputTaskStates.get(inputTaskIndex);
        }

        @Override
        public TaskState getOutputTaskState(int outputTaskIndex)
        {
            return outputTaskStates.get(outputTaskIndex);
        }

        public boolean isAllCommitted()
        {
            if (outputTaskStates == null) {
                // not initialized
                return false;
            }
            for (TaskState outputTaskState : outputTaskStates) {
                if (!outputTaskState.isCommitted()) {
                    return false;
                }
            }
            return true;
        }

        public boolean isAnyStarted()
        {
            if (inputTaskStates == null) {
                // not initialized
                return false;
            }
            for (TaskState inputTaskState : inputTaskStates) {
                if (inputTaskState.isStarted()) {
                    return true;
                }
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

        private List<Optional<CommitReport>> getInputCommitReports()
        {
            ImmutableList.Builder<Optional<CommitReport>> builder = ImmutableList.builder();
            for (TaskState inputTaskState : inputTaskStates) {
                builder.add(inputTaskState.getCommitReport());
            }
            return builder.build();
        }

        private List<Optional<CommitReport>> getOutputCommitReports()
        {
            ImmutableList.Builder<Optional<CommitReport>> builder = ImmutableList.builder();
            for (TaskState outputTaskState : outputTaskStates) {
                builder.add(outputTaskState.getCommitReport());
            }
            return builder.build();
        }

        public List<CommitReport> getAllInputCommitReports()
        {
            ImmutableList.Builder<CommitReport> builder = ImmutableList.builder();
            for (TaskState inputTaskState : inputTaskStates) {
                builder.add(inputTaskState.getCommitReport().get());
            }
            return builder.build();
        }

        public List<CommitReport> getAllOutputCommitReports()
        {
            ImmutableList.Builder<CommitReport> builder = ImmutableList.builder();
            for (TaskState outputTaskState : outputTaskStates) {
                builder.add(outputTaskState.getCommitReport().get());
            }
            return builder.build();
        }

        public List<Throwable> getExceptions()
        {
            ImmutableList.Builder<Throwable> builder = ImmutableList.builder();
            if (inputTaskStates != null) {  // null if not initialized yet
                for (TaskState inputTaskState : inputTaskStates) {
                    Optional<Throwable> exception = inputTaskState.getException();
                    if (exception.isPresent()) {
                        builder.add(exception.get());
                    }
                }
            }
            if (outputTaskStates != null) {  // null if not initialized yet
                for (TaskState outputTaskState : outputTaskStates) {
                    Optional<Throwable> exception = outputTaskState.getException();
                    if (exception.isPresent()) {
                        builder.add(exception.get());
                    }
                }
            }
            return builder.build();
        }

        public RuntimeException getRepresentativeException()
        {
            RuntimeException top = null;
            for (Throwable ex : getExceptions()) {
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
            for (Throwable e : getExceptions()) {
                ignoredExceptions.add(e);
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
                    first(schemas), executorSchema,
                    getInputCommitReports(), getOutputCommitReports());
        }

        public PartialExecutionException buildPartialExecuteException(Throwable cause, ExecSession exec)
        {
            return new PartialExecutionException(cause, buildResumeState(exec));
        }
    }

    protected ExecutorPlugin newExecutorPlugin(BulkLoaderTask task)
    {
        return Exec.newPlugin(ExecutorPlugin.class,
                task.getExecConfig().get(PluginType.class, "type", new PluginType("local")));
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
        } catch (ExecutionException ex) {
            throw Throwables.propagate(ex.getCause());
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
        } catch (ExecutionException ex) {
            throw Throwables.propagate(ex.getCause());
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
        } catch (ExecutionException ex) {
            throw Throwables.propagate(ex.getCause());
        }
    }

    public void doCleanup(ConfigSource config, ResumeState resume)
    {
        BulkLoaderTask task = config.loadConfig(BulkLoaderTask.class);
        InputPlugin inputPlugin = newInputPlugin(task);
        OutputPlugin outputPlugin = newOutputPlugin(task);

        ImmutableList.Builder<CommitReport> successfulInputCommitReports = ImmutableList.builder();
        ImmutableList.Builder<CommitReport> successfulOutputCommitReports = ImmutableList.builder();
        for (Optional<CommitReport> inputCommitReport : resume.getInputCommitReports()) {
            if (inputCommitReport.isPresent()) {
                successfulInputCommitReports.add(inputCommitReport.get());
            }
        }
        for (Optional<CommitReport> outputCommitReport : resume.getOutputCommitReports()) {
            if (outputCommitReport.isPresent()) {
                successfulOutputCommitReports.add(outputCommitReport.get());
            }
        }

        inputPlugin.cleanup(resume.getInputTaskSource(), resume.getInputSchema(),
                resume.getInputCommitReports().size(), successfulInputCommitReports.build());

        outputPlugin.cleanup(resume.getOutputTaskSource(), resume.getOutputSchema(),
                resume.getOutputCommitReports().size(), successfulOutputCommitReports.build());
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
            ConfigDiff inputConfigDiff = inputPlugin.transaction(task.getInputConfig(), new InputPlugin.Control() {
                public List<CommitReport> run(final TaskSource inputTask, final Schema inputSchema, final int inputTaskCount)
                {
                    state.setInputTaskSource(inputTask);
                    Filters.transaction(filterPlugins, task.getFilterConfigs(), inputSchema, new Filters.Control() {
                        public void run(final List<TaskSource> filterTasks, final List<Schema> schemas)
                        {
                            state.setSchemas(schemas);
                            state.setFilterTaskSources(filterTasks);
                            exec.transaction(task.getExecConfig(), last(schemas), inputTaskCount, new ExecutorPlugin.Control() {
                                public void transaction(final Schema executorSchema, final int outputTaskCount, final ExecutorPlugin.Executor executor)
                                {
                                    state.setExecutorSchema(executorSchema);
                                    ConfigDiff outputConfigDiff = outputPlugin.transaction(task.getOutputConfig(), executorSchema, outputTaskCount, new OutputPlugin.Control() {
                                        public List<CommitReport> run(final TaskSource outputTask)
                                        {
                                            state.setOutputTaskSource(outputTask);

                                            state.initialize(inputTaskCount, outputTaskCount);

                                            if (!state.isAllCommitted()) {  // inputTaskCount == 0
                                                execute(task, executor, state);
                                            }

                                            return state.getAllOutputCommitReports();
                                        }
                                    });
                                    state.setOutputConfigDiff(outputConfigDiff);
                                }
                            });
                        }
                    });
                    return state.getAllInputCommitReports();
                }
            });
            state.setInputConfigDiff(inputConfigDiff);

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
            ConfigDiff inputConfigDiff = inputPlugin.resume(resume.getInputTaskSource(), resume.getInputSchema(), resume.getInputCommitReports().size(), new InputPlugin.Control() {
                public List<CommitReport> run(final TaskSource inputTask, final Schema inputSchema, final int inputTaskCount)
                {
                    // TODO validate inputTask?
                    // TODO validate inputSchema
                    state.setInputTaskSource(inputTask);
                    Filters.transaction(filterPlugins, task.getFilterConfigs(), inputSchema, new Filters.Control() {
                        public void run(final List<TaskSource> filterTasks, final List<Schema> schemas)
                        {
                            state.setSchemas(schemas);
                            state.setFilterTaskSources(filterTasks);
                            exec.transaction(task.getExecConfig(), last(schemas), inputTaskCount, new ExecutorPlugin.Control() {
                                public void transaction(final Schema executorSchema, final int outputTaskCount, final ExecutorPlugin.Executor executor)
                                {
                                    // TODO validate executorSchema
                                    state.setExecutorSchema(executorSchema);
                                    ConfigDiff outputConfigDiff = outputPlugin.resume(resume.getOutputTaskSource(), executorSchema, outputTaskCount, new OutputPlugin.Control() {
                                        public List<CommitReport> run(final TaskSource outputTask)
                                        {
                                            // TODO validate outputTask?
                                            state.setOutputTaskSource(outputTask);

                                            restoreResumedCommitReports(resume, state);
                                            if (!state.isAllCommitted()) {
                                                execute(task, executor, state);
                                            }

                                            return state.getAllOutputCommitReports();
                                        }
                                    });
                                    state.setOutputConfigDiff(outputConfigDiff);
                                }
                            });
                        }
                    });
                    return state.getAllInputCommitReports();
                }
            });
            state.setInputConfigDiff(inputConfigDiff);

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

    private static void restoreResumedCommitReports(ResumeState resume, LoaderState state)
    {
        int inputTaskCount = resume.getInputCommitReports().size();
        int outputTaskCount = resume.getOutputCommitReports().size();

        state.initialize(inputTaskCount, outputTaskCount);

        for (int i=0; i < inputTaskCount; i++) {
            Optional<CommitReport> report = resume.getInputCommitReports().get(i);
            if (report.isPresent()) {
                TaskState task = state.getInputTaskState(i);
                task.start();
                task.setCommitReport(report.get());
                task.finish();
            }
        }

        for (int i=0; i < outputTaskCount; i++) {
            Optional<CommitReport> report = resume.getOutputCommitReports().get(i);
            if (report.isPresent()) {
                TaskState task = state.getOutputTaskState(i);
                task.start();
                task.setCommitReport(report.get());
                task.finish();
            }
        }
    }

    private void execute(BulkLoaderTask task, ExecutorPlugin.Executor executor, LoaderState state)
    {
        ProcessTask procTask = state.buildProcessTask();

        executor.execute(procTask, state);

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
