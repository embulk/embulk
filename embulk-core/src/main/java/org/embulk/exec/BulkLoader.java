package org.embulk.exec;

import java.util.List;
import java.util.concurrent.ExecutionException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.common.base.Throwables;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigException;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.TaskReport;
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

    protected static class LoaderState
            implements ProcessState
    {
        private final Logger logger;

        private final ProcessPluginSet plugins;

        private volatile TaskSource inputTaskSource;
        private volatile TaskSource outputTaskSource;
        private volatile List<TaskSource> filterTaskSources;
        private volatile List<Schema> schemas;
        private volatile Schema executorSchema;
        private volatile TransactionStage transactionStage;

        private volatile ConfigDiff inputConfigDiff;
        private volatile ConfigDiff outputConfigDiff;

        private volatile List<TaskState> inputTaskStates;
        private volatile List<TaskState> outputTaskStates;

        public LoaderState(Logger logger, ProcessPluginSet plugins)
        {
            this.logger = logger;
            this.plugins = plugins;
        }

        public Logger getLogger()
        {
            return logger;
        }

        public void setSchemas(List<Schema> schemas)
        {
            this.schemas = schemas;
        }

        public void setExecutorSchema(Schema executorSchema)
        {
            this.executorSchema = executorSchema;
        }

        public void setTransactionStage(TransactionStage transactionStage)
        {
            this.transactionStage = transactionStage;
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
                    plugins.getInputPluginType(), plugins.getOutputPluginType(), plugins.getFilterPluginTypes(),
                    inputTaskSource, outputTaskSource, filterTaskSources,
                    schemas, executorSchema, Exec.newTaskSource());
        }

        @Override
        public void initialize(int inputTaskCount, int outputTaskCount)
        {
            if (inputTaskStates != null || outputTaskStates != null) {
                // initialize is called twice if resume (by restoreResumedTaskReports and ExecutorPlugin.execute)
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

        public boolean isAllTasksCommitted()
        {
            // here can't assume that input tasks are committed when output tasks are
            // committed because that's controlled by executor plugins. some executor
            // plugins (especially mapreduce executor) may commit output tasks even
            // when some input tasks failed. This is asemantically allowed behavior for
            // executor plugins (as long as output plugin is atomic and idempotent).
            if (inputTaskStates == null || outputTaskStates == null) {
                // not initialized
                return false;
            }
            for (TaskState inputTaskState : inputTaskStates) {
                if (!inputTaskState.isCommitted()) {
                    return false;
                }
            }
            for (TaskState outputTaskState : outputTaskStates) {
                if (!outputTaskState.isCommitted()) {
                    return false;
                }
            }
            return true;
        }

        public int countUncommittedInputTasks()
        {
            if (inputTaskStates == null) {
                // not initialized
                return 0;
            }
            int count = 0;
            for (TaskState inputTaskState : inputTaskStates) {
                if (!inputTaskState.isCommitted()) {
                    count++;
                }
            }
            return count;
        }

        public int countUncommittedOutputTasks()
        {
            if (outputTaskStates == null) {
                // not initialized
                return 0;
            }
            int count = 0;
            for (TaskState outputTaskState : outputTaskStates) {
                if (!outputTaskState.isCommitted()) {
                    count++;
                }
            }
            return count;
        }

        public boolean isAllTransactionsCommitted()
        {
            return inputConfigDiff != null && outputConfigDiff != null;
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

        private List<Optional<TaskReport>> getInputTaskReports()
        {
            ImmutableList.Builder<Optional<TaskReport>> builder = ImmutableList.builder();
            for (TaskState inputTaskState : inputTaskStates) {
                builder.add(inputTaskState.getTaskReport());
            }
            return builder.build();
        }

        private List<Optional<TaskReport>> getOutputTaskReports()
        {
            ImmutableList.Builder<Optional<TaskReport>> builder = ImmutableList.builder();
            for (TaskState outputTaskState : outputTaskStates) {
                builder.add(outputTaskState.getTaskReport());
            }
            return builder.build();
        }

        public List<TaskReport> getAllInputTaskReports()
        {
            ImmutableList.Builder<TaskReport> builder = ImmutableList.builder();
            for (TaskState inputTaskState : inputTaskStates) {
                builder.add(inputTaskState.getTaskReport().get());
            }
            return builder.build();
        }

        public List<TaskReport> getAllOutputTaskReports()
        {
            ImmutableList.Builder<TaskReport> builder = ImmutableList.builder();
            for (TaskState outputTaskState : outputTaskStates) {
                builder.add(outputTaskState.getTaskReport().get());
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

            return new ExecutionResult(configDiff, false, ignoredExceptions.build());
        }

        public ExecutionResult buildExecuteResultOfSkippedExecution(ConfigDiff configDiff)
        {
            ImmutableList.Builder<Throwable> ignoredExceptions = ImmutableList.builder();
            for (Throwable e : getExceptions()) {
                ignoredExceptions.add(e);
            }

            return new ExecutionResult(configDiff, true, ignoredExceptions.build());
        }

        public ResumeState buildResumeState(ExecSession exec)
        {
            Schema inputSchema = (schemas == null) ? null : schemas.get(0);
            List<Optional<TaskReport>> inputTaskReports = (inputTaskStates == null) ? null : getInputTaskReports();
            List<Optional<TaskReport>> outputTaskReports = (outputTaskStates == null) ? null : getOutputTaskReports();
            return new ResumeState(
                    exec.getSessionExecConfig(),
                    inputTaskSource, outputTaskSource,
                    inputSchema, executorSchema,
                    inputTaskReports, outputTaskReports);
        }

        public PartialExecutionException buildPartialExecuteException(Throwable cause, ExecSession exec)
        {
            return new PartialExecutionException(cause, buildResumeState(exec), transactionStage);
        }
    }

    protected LoaderState newLoaderState(Logger logger, ProcessPluginSet plugins)
    {
        return new LoaderState(logger, plugins);
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
            ExecSession exec = ExecSession.builder(injector).fromExecConfig(resume.getExecSessionConfigSource()).build();
            ExecutionResult result = Exec.doWith(exec, new ExecAction<ExecutionResult>() {
                public ExecutionResult run()
                {
                    try (SetCurrentThreadName dontCare = new SetCurrentThreadName("resume")) {
                        return doResume(config, resume);
                    }
                }
            });
            exec.cleanup();
            return result;
        } catch (ExecutionException ex) {
            throw Throwables.propagate(ex.getCause());
        }
    }

    public void cleanup(final ConfigSource config, final ResumeState resume)
    {
        try {
            ExecSession exec = ExecSession.builder(injector).fromExecConfig(resume.getExecSessionConfigSource()).build();
            Exec.doWith(exec, new ExecAction<Void>() {
                public Void run()
                {
                    try (SetCurrentThreadName dontCare = new SetCurrentThreadName("cleanup")) {
                        doCleanup(config, resume);
                        return null;
                    }
                }
            });
            exec.cleanup();
        } catch (ExecutionException ex) {
            throw Throwables.propagate(ex.getCause());
        }
    }

    protected static class ProcessPluginSet
    {
        private final PluginType inputPluginType;
        private final PluginType outputPluginType;
        private final List<PluginType> filterPluginTypes;

        private final InputPlugin inputPlugin;
        private final OutputPlugin outputPlugin;
        private final List<FilterPlugin> filterPlugins;

        public ProcessPluginSet(BulkLoaderTask task)
        {
            this.inputPluginType = task.getInputConfig().get(PluginType.class, "type");
            this.outputPluginType = task.getOutputConfig().get(PluginType.class, "type");
            this.filterPluginTypes = Filters.getPluginTypes(task.getFilterConfigs());
            this.inputPlugin = Exec.newPlugin(InputPlugin.class, inputPluginType);
            this.outputPlugin = Exec.newPlugin(OutputPlugin.class, outputPluginType);
            this.filterPlugins = Filters.newFilterPlugins(Exec.session(), filterPluginTypes);
        }

        public PluginType getInputPluginType()
        {
            return inputPluginType;
        }

        public PluginType getOutputPluginType()
        {
            return outputPluginType;
        }

        public List<PluginType> getFilterPluginTypes()
        {
            return filterPluginTypes;
        }

        public InputPlugin getInputPlugin()
        {
            return inputPlugin;
        }

        public OutputPlugin getOutputPlugin()
        {
            return outputPlugin;
        }

        public List<FilterPlugin> getFilterPlugins()
        {
            return filterPlugins;
        }
    }

    public void doCleanup(ConfigSource config, ResumeState resume)
    {
        BulkLoaderTask task = config.loadConfig(BulkLoaderTask.class);
        ProcessPluginSet plugins = new ProcessPluginSet(task);  // TODO don't create filter plugins

        ImmutableList.Builder<TaskReport> successfulInputTaskReports = ImmutableList.builder();
        ImmutableList.Builder<TaskReport> successfulOutputTaskReports = ImmutableList.builder();
        for (Optional<TaskReport> inputTaskReport : resume.getInputTaskReports()) {
            if (inputTaskReport.isPresent()) {
                successfulInputTaskReports.add(inputTaskReport.get());
            }
        }
        for (Optional<TaskReport> outputTaskReport : resume.getOutputTaskReports()) {
            if (outputTaskReport.isPresent()) {
                successfulOutputTaskReports.add(outputTaskReport.get());
            }
        }

        plugins.getInputPlugin().cleanup(resume.getInputTaskSource(), resume.getInputSchema(),
                resume.getInputTaskReports().size(), successfulInputTaskReports.build());

        plugins.getOutputPlugin().cleanup(resume.getOutputTaskSource(), resume.getOutputSchema(),
                resume.getOutputTaskReports().size(), successfulOutputTaskReports.build());
    }

    private ExecutorPlugin newExecutorPlugin(BulkLoaderTask task)
    {
        return Exec.newPlugin(ExecutorPlugin.class,
                task.getExecConfig().get(PluginType.class, "type", new PluginType("local")));
    }

    private ExecutionResult doRun(ConfigSource config)
    {
        final BulkLoaderTask task = config.loadConfig(BulkLoaderTask.class);

        final ExecutorPlugin exec = newExecutorPlugin(task);
        final ProcessPluginSet plugins = new ProcessPluginSet(task);

        final LoaderState state = newLoaderState(Exec.getLogger(BulkLoader.class), plugins);
        state.setTransactionStage(TransactionStage.INPUT_BEGIN);
        try {
            ConfigDiff inputConfigDiff = plugins.getInputPlugin().transaction(task.getInputConfig(), new InputPlugin.Control() {
                public List<TaskReport> run(final TaskSource inputTask, final Schema inputSchema, final int inputTaskCount)
                {
                    state.setInputTaskSource(inputTask);
                    state.setTransactionStage(TransactionStage.FILTER_BEGIN);
                    Filters.transaction(plugins.getFilterPlugins(), task.getFilterConfigs(), inputSchema, new Filters.Control() {
                        public void run(final List<TaskSource> filterTasks, final List<Schema> schemas)
                        {
                            state.setSchemas(schemas);
                            state.setFilterTaskSources(filterTasks);
                            state.setTransactionStage(TransactionStage.EXECUTOR_BEGIN);
                            exec.transaction(task.getExecConfig(), last(schemas), inputTaskCount, new ExecutorPlugin.Control() {
                                public void transaction(final Schema executorSchema, final int outputTaskCount, final ExecutorPlugin.Executor executor)
                                {
                                    state.setExecutorSchema(executorSchema);
                                    state.setTransactionStage(TransactionStage.OUTPUT_BEGIN);
                                    ConfigDiff outputConfigDiff = plugins.getOutputPlugin().transaction(task.getOutputConfig(), executorSchema, outputTaskCount, new OutputPlugin.Control() {
                                        public List<TaskReport> run(final TaskSource outputTask)
                                        {
                                            state.setOutputTaskSource(outputTask);
                                            state.initialize(inputTaskCount, outputTaskCount);
                                            state.setTransactionStage(TransactionStage.RUN);

                                            if (!state.isAllTasksCommitted()) {  // inputTaskCount == 0
                                                execute(task, executor, state);
                                            }

                                            if (!state.isAllTasksCommitted()) {
                                                throw new RuntimeException(String.format("%d input tasks and %d output tasks failed",
                                                            state.countUncommittedInputTasks(), state.countUncommittedOutputTasks()));
                                            }

                                            state.setTransactionStage(TransactionStage.OUTPUT_COMMIT);
                                            return state.getAllOutputTaskReports();
                                        }
                                    });
                                    state.setOutputConfigDiff(outputConfigDiff);
                                    state.setTransactionStage(TransactionStage.EXECUTOR_COMMIT);
                                }
                            });
                            state.setTransactionStage(TransactionStage.FILTER_COMMIT);
                        }
                    });
                    state.setTransactionStage(TransactionStage.INPUT_COMMIT);
                    return state.getAllInputTaskReports();
                }
            });
            state.setInputConfigDiff(inputConfigDiff);
            state.setTransactionStage(TransactionStage.CLEANUP);

            cleanupCommittedTransaction(config, state);

            return state.buildExecuteResult();

        } catch (Throwable ex) {
            if (isSkippedTransaction(ex)) {
                ConfigDiff configDiff = ((SkipTransactionException) ex).getConfigDiff();
                return state.buildExecuteResultOfSkippedExecution(configDiff);
            }
            else if (state.isAllTasksCommitted() && state.isAllTransactionsCommitted()) {
                // ignore the exception
                return state.buildExecuteResultWithWarningException(ex);
            }
            throw state.buildPartialExecuteException(ex, Exec.session());
        }
    }

    private ExecutionResult doResume(ConfigSource config, final ResumeState resume)
    {
        final BulkLoaderTask task = config.loadConfig(BulkLoaderTask.class);

        final ExecutorPlugin exec = newExecutorPlugin(task);
        final ProcessPluginSet plugins = new ProcessPluginSet(task);

        final LoaderState state = newLoaderState(Exec.getLogger(BulkLoader.class), plugins);
        state.setTransactionStage(TransactionStage.INPUT_BEGIN);
        try {
            ConfigDiff inputConfigDiff = plugins.getInputPlugin().resume(resume.getInputTaskSource(), resume.getInputSchema(), resume.getInputTaskReports().size(), new InputPlugin.Control() {
                public List<TaskReport> run(final TaskSource inputTask, final Schema inputSchema, final int inputTaskCount)
                {
                    // TODO validate inputTask?
                    // TODO validate inputSchema
                    state.setInputTaskSource(inputTask);
                    state.setTransactionStage(TransactionStage.FILTER_BEGIN);
                    Filters.transaction(plugins.getFilterPlugins(), task.getFilterConfigs(), inputSchema, new Filters.Control() {
                        public void run(final List<TaskSource> filterTasks, final List<Schema> schemas)
                        {
                            state.setSchemas(schemas);
                            state.setFilterTaskSources(filterTasks);
                            state.setTransactionStage(TransactionStage.EXECUTOR_BEGIN);
                            exec.transaction(task.getExecConfig(), last(schemas), inputTaskCount, new ExecutorPlugin.Control() {
                                public void transaction(final Schema executorSchema, final int outputTaskCount, final ExecutorPlugin.Executor executor)
                                {
                                    // TODO validate executorSchema
                                    state.setExecutorSchema(executorSchema);
                                    state.setTransactionStage(TransactionStage.OUTPUT_BEGIN);
                                    ConfigDiff outputConfigDiff = plugins.getOutputPlugin().resume(resume.getOutputTaskSource(), executorSchema, outputTaskCount, new OutputPlugin.Control() {
                                        public List<TaskReport> run(final TaskSource outputTask)
                                        {
                                            // TODO validate outputTask?
                                            state.setOutputTaskSource(outputTask);
                                            restoreResumedTaskReports(resume, state);
                                            state.setTransactionStage(TransactionStage.RUN);

                                            if (!state.isAllTasksCommitted()) {
                                                execute(task, executor, state);
                                            }

                                            if (!state.isAllTasksCommitted()) {
                                                throw new RuntimeException(String.format("%d input tasks and %d output tasks failed",
                                                            state.countUncommittedInputTasks(), state.countUncommittedOutputTasks()));
                                            }

                                            state.setTransactionStage(TransactionStage.OUTPUT_COMMIT);
                                            return state.getAllOutputTaskReports();
                                        }
                                    });
                                    state.setOutputConfigDiff(outputConfigDiff);
                                    state.setTransactionStage(TransactionStage.EXECUTOR_COMMIT);
                                }
                            });
                            state.setTransactionStage(TransactionStage.FILTER_COMMIT);
                        }
                    });
                    state.setTransactionStage(TransactionStage.INPUT_COMMIT);
                    return state.getAllInputTaskReports();
                }
            });
            state.setInputConfigDiff(inputConfigDiff);
            state.setTransactionStage(TransactionStage.CLEANUP);

            cleanupCommittedTransaction(config, state);

            return state.buildExecuteResult();

        } catch (Throwable ex) {
            if (isSkippedTransaction(ex)) {
                ConfigDiff configDiff = ((SkipTransactionException) ex).getConfigDiff();
                return state.buildExecuteResultOfSkippedExecution(configDiff);
            }
            else if (state.isAllTasksCommitted() && state.isAllTransactionsCommitted()) {
                // ignore the exception
                return state.buildExecuteResultWithWarningException(ex);
            }
            throw state.buildPartialExecuteException(ex, Exec.session());
        }
    }

    private static boolean isSkippedTransaction(Throwable ex)
    {
        return ex instanceof SkipTransactionException;
    }

    private static void restoreResumedTaskReports(ResumeState resume, LoaderState state)
    {
        int inputTaskCount = resume.getInputTaskReports().size();
        int outputTaskCount = resume.getOutputTaskReports().size();

        state.initialize(inputTaskCount, outputTaskCount);

        for (int i=0; i < inputTaskCount; i++) {
            Optional<TaskReport> report = resume.getInputTaskReports().get(i);
            if (report.isPresent()) {
                TaskState task = state.getInputTaskState(i);
                task.start();
                task.setTaskReport(report.get());
                task.finish();
            }
        }

        for (int i=0; i < outputTaskCount; i++) {
            Optional<TaskReport> report = resume.getOutputTaskReports().get(i);
            if (report.isPresent()) {
                TaskState task = state.getOutputTaskState(i);
                task.start();
                task.setTaskReport(report.get());
                task.finish();
            }
        }
    }

    private void execute(BulkLoaderTask task, ExecutorPlugin.Executor executor, LoaderState state)
    {
        ProcessTask procTask = state.buildProcessTask();

        executor.execute(procTask, state);

        if (!state.isAllTasksCommitted()) {
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
