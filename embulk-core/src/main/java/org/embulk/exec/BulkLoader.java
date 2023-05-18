package org.embulk.exec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.embulk.EmbulkSystemProperties;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.plugin.PluginType;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecAction;
import org.embulk.spi.ExecInternal;
import org.embulk.spi.ExecSessionInternal;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.FileOutputRunner;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ProcessState;
import org.embulk.spi.ProcessTask;
import org.embulk.spi.Schema;
import org.embulk.spi.TaskState;
import org.embulk.spi.util.FiltersInternal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkLoader {
    private final EmbulkSystemProperties embulkSystemProperties;

    public interface BulkLoaderTask extends Task {
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

    public BulkLoader(final EmbulkSystemProperties embulkSystemProperties) {
        this.embulkSystemProperties = embulkSystemProperties;
    }

    protected static class LoaderState implements ProcessState {
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

        public LoaderState(Logger logger, ProcessPluginSet plugins) {
            this.logger = logger;
            this.plugins = plugins;
        }

        public Logger getLogger() {
            return logger;
        }

        public void setSchemas(List<Schema> schemas) {
            this.schemas = schemas;
        }

        public void setExecutorSchema(Schema executorSchema) {
            this.executorSchema = executorSchema;
        }

        public void setTransactionStage(TransactionStage transactionStage) {
            this.transactionStage = transactionStage;
        }

        public void setInputTaskSource(TaskSource inputTaskSource) {
            this.inputTaskSource = inputTaskSource;
        }

        public void setOutputTaskSource(TaskSource outputTaskSource) {
            this.outputTaskSource = outputTaskSource;
        }

        public void setFilterTaskSources(List<TaskSource> filterTaskSources) {
            this.filterTaskSources = filterTaskSources;
        }

        public ProcessTask buildProcessTask() {
            return new ProcessTask(
                    plugins.getInputPluginType(), plugins.getOutputPluginType(), plugins.getFilterPluginTypes(),
                    inputTaskSource, outputTaskSource, filterTaskSources,
                    schemas, executorSchema, Exec.newTaskSource());
        }

        @Override
        public void initialize(int inputTaskCount, int outputTaskCount) {
            if (inputTaskStates != null || outputTaskStates != null) {
                // initialize is called twice if resume (by restoreResumedTaskReports and ExecutorPlugin.execute)
                if (inputTaskStates.size() != inputTaskCount || outputTaskStates.size() != outputTaskCount) {
                    throw new ConfigException(String.format(
                            "input task count and output task (%d and %d) must be same with the first execution (%d and %d) whenre resumed",
                            inputTaskCount, outputTaskCount, inputTaskStates.size(), outputTaskStates.size()));
                }
            } else {
                final ArrayList<TaskState> inputTaskStates = new ArrayList<>();
                final ArrayList<TaskState> outputTaskStates = new ArrayList<>();
                for (int i = 0; i < inputTaskCount; i++) {
                    inputTaskStates.add(new TaskState());
                }
                for (int i = 0; i < outputTaskCount; i++) {
                    outputTaskStates.add(new TaskState());
                }
                this.inputTaskStates = Collections.unmodifiableList(inputTaskStates);
                this.outputTaskStates = Collections.unmodifiableList(outputTaskStates);
            }
        }

        @Override
        public TaskState getInputTaskState(int inputTaskIndex) {
            return inputTaskStates.get(inputTaskIndex);
        }

        @Override
        public TaskState getOutputTaskState(int outputTaskIndex) {
            return outputTaskStates.get(outputTaskIndex);
        }

        public boolean isAllTasksCommitted() {
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

        public int countUncommittedInputTasks() {
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

        public int countUncommittedOutputTasks() {
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

        public boolean isAllTransactionsCommitted() {
            return inputConfigDiff != null && outputConfigDiff != null;
        }

        public void setOutputConfigDiff(ConfigDiff outputConfigDiff) {
            if (outputConfigDiff == null) {
                outputConfigDiff = Exec.newConfigDiff();
            }
            this.outputConfigDiff = outputConfigDiff;
        }

        public void setInputConfigDiff(ConfigDiff inputConfigDiff) {
            if (inputConfigDiff == null) {
                inputConfigDiff = Exec.newConfigDiff();
            }
            this.inputConfigDiff = inputConfigDiff;
        }

        private List<Optional<TaskReport>> getInputTaskReports() {
            final ArrayList<Optional<TaskReport>> builder = new ArrayList<>();
            for (TaskState inputTaskState : inputTaskStates) {
                builder.add(inputTaskState.getTaskReport());
            }
            return Collections.unmodifiableList(builder);
        }

        private List<Optional<TaskReport>> getOutputTaskReports() {
            final ArrayList<Optional<TaskReport>> builder = new ArrayList<>();
            for (TaskState outputTaskState : outputTaskStates) {
                builder.add(outputTaskState.getTaskReport());
            }
            return Collections.unmodifiableList(builder);
        }

        public List<TaskReport> getAllInputTaskReports() {
            final ArrayList<TaskReport> builder = new ArrayList<>();
            for (TaskState inputTaskState : inputTaskStates) {
                builder.add(inputTaskState.getTaskReport().get());
            }
            return Collections.unmodifiableList(builder);
        }

        public List<TaskReport> getAllOutputTaskReports() {
            final ArrayList<TaskReport> builder = new ArrayList<>();
            for (TaskState outputTaskState : outputTaskStates) {
                builder.add(outputTaskState.getTaskReport().get());
            }
            return Collections.unmodifiableList(builder);
        }

        public List<Throwable> getExceptions() {
            final ArrayList<Throwable> builder = new ArrayList<>();
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
            return Collections.unmodifiableList(builder);
        }

        public RuntimeException getRepresentativeException() {
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

        public ExecutionResult buildExecuteResult() {
            return buildExecuteResultWithWarningException(null);
        }

        public ExecutionResult buildExecuteResultWithWarningException(Throwable ex) {
            ConfigDiff configDiff = Exec.newConfigDiff();
            if (inputConfigDiff != null) {
                configDiff.getNestedOrSetEmpty("in").merge(inputConfigDiff);
            }
            if (outputConfigDiff != null) {
                configDiff.getNestedOrSetEmpty("out").merge(outputConfigDiff);
            }

            final ArrayList<Throwable> ignoredExceptions = new ArrayList<>();
            for (Throwable e : getExceptions()) {
                ignoredExceptions.add(e);
            }
            if (ex != null) {
                ignoredExceptions.add(ex);
            }

            return new ExecutionResult(configDiff, false, Collections.unmodifiableList(ignoredExceptions),
                    this.getAllInputTaskReports(), this.getAllOutputTaskReports());
        }

        public ExecutionResult buildExecuteResultOfSkippedExecution(ConfigDiff configDiff) {
            final ArrayList<Throwable> ignoredExceptions = new ArrayList<>();
            for (Throwable e : getExceptions()) {
                ignoredExceptions.add(e);
            }

            return new ExecutionResult(configDiff, true, Collections.unmodifiableList(ignoredExceptions),
                    this.getAllInputTaskReports(), this.getAllOutputTaskReports());
        }

        public ResumeState buildResumeState(ExecSessionInternal exec) {
            Schema inputSchema = (schemas == null) ? null : schemas.get(0);
            List<Optional<TaskReport>> inputTaskReports = (inputTaskStates == null) ? null : getInputTaskReports();
            List<Optional<TaskReport>> outputTaskReports = (outputTaskStates == null) ? null : getOutputTaskReports();
            return new ResumeState(
                    exec.newConfigSource().set("transaction_time", exec.getTransactionTimeString()),
                    inputTaskSource, outputTaskSource,
                    inputSchema, executorSchema,
                    inputTaskReports, outputTaskReports);
        }

        public PartialExecutionException buildPartialExecuteException(Throwable cause, ExecSessionInternal exec) {
            return new PartialExecutionException(cause, buildResumeState(exec), transactionStage);
        }
    }

    protected LoaderState newLoaderState(Logger logger, ProcessPluginSet plugins) {
        return new LoaderState(logger, plugins);
    }

    public ExecutionResult run(ExecSessionInternal exec, final ConfigSource config) {
        try {
            return ExecInternal.doWith(exec, new ExecAction<ExecutionResult>() {
                    public ExecutionResult run() {
                        try (SetCurrentThreadName dontCare = new SetCurrentThreadName("transaction")) {
                            return doRun(config);
                        }
                    }
                });
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException) ex.getCause();
            }
            if (ex.getCause() instanceof Error) {
                throw (Error) ex.getCause();
            }
            throw new RuntimeException(ex.getCause());
        }
    }

    @Deprecated
    public ExecutionResult resume(final ConfigSource config, final ResumeState resume) {
        throw new UnsupportedOperationException(
                "BulkLoader#resume(ConfigSource, ResumeState) is no longer supported. "
                + "Use BulkLoader#resume(ExecSessionInternal, ConfigSource, ResumeState) instead. "
                + "Plugins should not call those methods anyway, though.");
    }

    public ExecutionResult resume(final ExecSessionInternal exec, final ConfigSource config, final ResumeState resume) {
        try {
            ExecutionResult result = ExecInternal.doWith(exec, new ExecAction<ExecutionResult>() {
                    public ExecutionResult run() {
                        try (SetCurrentThreadName dontCare = new SetCurrentThreadName("resume")) {
                            return doResume(config, resume);
                        }
                    }
                });
            exec.cleanup();
            return result;
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException) ex.getCause();
            }
            if (ex.getCause() instanceof Error) {
                throw (Error) ex.getCause();
            }
            throw new RuntimeException(ex.getCause());
        }
    }

    @Deprecated
    public void cleanup(final ConfigSource config, final ResumeState resume) {
        throw new UnsupportedOperationException(
                "BulkLoader#cleanup(ConfigSource, ResumeState) is no longer supported. "
                + "Use BulkLoader#cleanup(ExecSessionInternal, ConfigSource, ResumeState) instead. "
                + "Plugins should not call those methods anyway, though.");
    }

    public void cleanup(final ExecSessionInternal exec, final ConfigSource config, final ResumeState resume) {
        try {
            ExecInternal.doWith(exec, new ExecAction<Void>() {
                    public Void run() {
                        try (SetCurrentThreadName dontCare = new SetCurrentThreadName("cleanup")) {
                            doCleanup(config, resume);
                            return null;
                        }
                    }
                });
            exec.cleanup();
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException) ex.getCause();
            }
            if (ex.getCause() instanceof Error) {
                throw (Error) ex.getCause();
            }
            throw new RuntimeException(ex.getCause());
        }
    }

    protected static class ProcessPluginSet {
        private final PluginType inputPluginType;
        private final PluginType outputPluginType;
        private final List<PluginType> filterPluginTypes;

        private final InputPlugin inputPlugin;
        private final OutputPlugin outputPlugin;
        private final List<FilterPlugin> filterPlugins;

        public ProcessPluginSet(BulkLoaderTask task) {
            this.inputPluginType = task.getInputConfig().get(PluginType.class, "type");
            this.outputPluginType = task.getOutputConfig().get(PluginType.class, "type");
            this.filterPluginTypes = FiltersInternal.getPluginTypes(task.getFilterConfigs());
            this.inputPlugin = ExecInternal.newPlugin(InputPlugin.class, inputPluginType);
            this.outputPlugin = ExecInternal.newPlugin(OutputPlugin.class, outputPluginType);
            this.filterPlugins = FiltersInternal.newFilterPlugins(ExecInternal.sessionInternal(), filterPluginTypes);
        }

        public PluginType getInputPluginType() {
            return inputPluginType;
        }

        public PluginType getOutputPluginType() {
            return outputPluginType;
        }

        public List<PluginType> getFilterPluginTypes() {
            return filterPluginTypes;
        }

        public InputPlugin getInputPlugin() {
            return inputPlugin;
        }

        public OutputPlugin getOutputPlugin() {
            return outputPlugin;
        }

        public List<FilterPlugin> getFilterPlugins() {
            return filterPlugins;
        }
    }

    public void doCleanup(ConfigSource config, ResumeState resume) {
        final BulkLoaderTask task = loadBulkLoaderTask(config);
        ProcessPluginSet plugins = new ProcessPluginSet(task);  // TODO don't create filter plugins

        final ArrayList<TaskReport> successfulInputTaskReports = new ArrayList<>();
        final ArrayList<TaskReport> successfulOutputTaskReports = new ArrayList<>();
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

        final TaskSource inputTaskSource;
        if (plugins.getInputPlugin() instanceof FileInputRunner) {
            inputTaskSource = FileInputRunner.getFileInputTaskSource(resume.getInputTaskSource());
        } else {
            inputTaskSource = resume.getInputTaskSource();
        }
        plugins.getInputPlugin().cleanup(inputTaskSource, resume.getInputSchema(),
                resume.getInputTaskReports().size(), Collections.unmodifiableList(successfulInputTaskReports));

        final TaskSource outputTaskSource;
        if (plugins.getOutputPlugin() instanceof FileOutputRunner) {
            outputTaskSource = FileOutputRunner.getFileOutputTaskSource(resume.getOutputTaskSource());
        } else {
            outputTaskSource = resume.getOutputTaskSource();
        }
        plugins.getOutputPlugin().cleanup(outputTaskSource, resume.getOutputSchema(),
                resume.getOutputTaskReports().size(), Collections.unmodifiableList(successfulOutputTaskReports));
    }

    private ExecutorPlugin newExecutorPlugin(BulkLoaderTask task) {
        return ExecInternal.newPlugin(ExecutorPlugin.class,
                task.getExecConfig().get(PluginType.class, "type", PluginType.LOCAL));
    }

    private ExecutionResult doRun(ConfigSource config) {
        final BulkLoaderTask task = loadBulkLoaderTask(config);

        final ExecutorPlugin exec = newExecutorPlugin(task);
        final ProcessPluginSet plugins = new ProcessPluginSet(task);

        final LoaderState state = newLoaderState(logger, plugins);
        state.setTransactionStage(TransactionStage.INPUT_BEGIN);
        try {
            ConfigDiff inputConfigDiff = plugins.getInputPlugin().transaction(task.getInputConfig(), new InputPlugin.Control() {
                public List<TaskReport> run(final TaskSource inputTask, final Schema inputSchema, final int inputTaskCount) {
                    state.setInputTaskSource(inputTask);
                    state.setTransactionStage(TransactionStage.FILTER_BEGIN);
                    FiltersInternal.transaction(plugins.getFilterPlugins(), task.getFilterConfigs(), inputSchema, new FiltersInternal.Control() {
                        public void run(final List<TaskSource> filterTasks, final List<Schema> schemas) {
                            state.setSchemas(schemas);
                            state.setFilterTaskSources(filterTasks);
                            state.setTransactionStage(TransactionStage.EXECUTOR_BEGIN);
                            exec.transaction(task.getExecConfig(), last(schemas), inputTaskCount, new ExecutorPlugin.Control() {
                                public void transaction(final Schema executorSchema, final int outputTaskCount, final ExecutorPlugin.Executor executor) {
                                    state.setExecutorSchema(executorSchema);
                                    state.setTransactionStage(TransactionStage.OUTPUT_BEGIN);
                                    @SuppressWarnings("checkstyle:LineLength")
                                    ConfigDiff outputConfigDiff = plugins.getOutputPlugin().transaction(task.getOutputConfig(), executorSchema, outputTaskCount, new OutputPlugin.Control() {
                                        public List<TaskReport> run(final TaskSource outputTask) {
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
            } else if (state.isAllTasksCommitted() && state.isAllTransactionsCommitted()) {
                // ignore the exception
                return state.buildExecuteResultWithWarningException(ex);
            }
            throw state.buildPartialExecuteException(ex, ExecInternal.sessionInternal());
        }
    }

    private ExecutionResult doResume(ConfigSource config, final ResumeState resume) {
        final BulkLoaderTask task = loadBulkLoaderTask(config);

        final ExecutorPlugin exec = newExecutorPlugin(task);
        final ProcessPluginSet plugins = new ProcessPluginSet(task);

        final LoaderState state = newLoaderState(logger, plugins);
        state.setTransactionStage(TransactionStage.INPUT_BEGIN);
        try {
            @SuppressWarnings("checkstyle:LineLength")
            ConfigDiff inputConfigDiff = plugins.getInputPlugin().resume(resume.getInputTaskSource(), resume.getInputSchema(), resume.getInputTaskReports().size(), new InputPlugin.Control() {
                public List<TaskReport> run(final TaskSource inputTask, final Schema inputSchema, final int inputTaskCount) {
                    // TODO validate inputTask?
                    // TODO validate inputSchema
                    state.setInputTaskSource(inputTask);
                    state.setTransactionStage(TransactionStage.FILTER_BEGIN);
                    FiltersInternal.transaction(plugins.getFilterPlugins(), task.getFilterConfigs(), inputSchema, new FiltersInternal.Control() {
                        public void run(final List<TaskSource> filterTasks, final List<Schema> schemas) {
                            state.setSchemas(schemas);
                            state.setFilterTaskSources(filterTasks);
                            state.setTransactionStage(TransactionStage.EXECUTOR_BEGIN);
                            exec.transaction(task.getExecConfig(), last(schemas), inputTaskCount, new ExecutorPlugin.Control() {
                                public void transaction(final Schema executorSchema, final int outputTaskCount, final ExecutorPlugin.Executor executor) {
                                    // TODO validate executorSchema
                                    state.setExecutorSchema(executorSchema);
                                    state.setTransactionStage(TransactionStage.OUTPUT_BEGIN);
                                    @SuppressWarnings("checkstyle:LineLength")
                                    ConfigDiff outputConfigDiff = plugins.getOutputPlugin().resume(resume.getOutputTaskSource(), executorSchema, outputTaskCount, new OutputPlugin.Control() {
                                        public List<TaskReport> run(final TaskSource outputTask) {
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
            } else if (state.isAllTasksCommitted() && state.isAllTransactionsCommitted()) {
                // ignore the exception
                return state.buildExecuteResultWithWarningException(ex);
            }
            throw state.buildPartialExecuteException(ex, ExecInternal.sessionInternal());
        }
    }

    private static boolean isSkippedTransaction(Throwable ex) {
        return ex instanceof SkipTransactionException;
    }

    private static void restoreResumedTaskReports(ResumeState resume, LoaderState state) {
        int inputTaskCount = resume.getInputTaskReports().size();
        int outputTaskCount = resume.getOutputTaskReports().size();

        state.initialize(inputTaskCount, outputTaskCount);

        for (int i = 0; i < inputTaskCount; i++) {
            Optional<TaskReport> report = resume.getInputTaskReports().get(i);
            if (report.isPresent()) {
                TaskState task = state.getInputTaskState(i);
                task.start();
                task.setTaskReport(report.get());
                task.finish();
            }
        }

        for (int i = 0; i < outputTaskCount; i++) {
            Optional<TaskReport> report = resume.getOutputTaskReports().get(i);
            if (report.isPresent()) {
                TaskState task = state.getOutputTaskState(i);
                task.start();
                task.setTaskReport(report.get());
                task.finish();
            }
        }
    }

    private void execute(BulkLoaderTask task, ExecutorPlugin.Executor executor, LoaderState state) {
        ProcessTask procTask = state.buildProcessTask();

        executor.execute(procTask, state);

        if (!state.isAllTasksCommitted()) {
            throw state.getRepresentativeException();
        }
    }

    private void cleanupCommittedTransaction(ConfigSource config, LoaderState state) {
        try {
            doCleanup(config, state.buildResumeState(ExecInternal.sessionInternal()));
        } catch (Exception ex) {
            state.getLogger().warn("Commit succeeded but cleanup failed. Ignoring this exception.", ex);  // TODO
        }
    }

    @SuppressWarnings("deprecation") // https://github.com/embulk/embulk/issues/1301
    private static BulkLoaderTask loadBulkLoaderTask(final ConfigSource config) {
        return config.loadConfig(BulkLoaderTask.class);
    }

    private static Schema first(List<Schema> schemas) {
        return schemas.get(0);
    }

    private static Schema last(List<Schema> schemas) {
        return schemas.get(schemas.size() - 1);
    }

    private static final Logger logger = LoggerFactory.getLogger(BulkLoader.class);
}
