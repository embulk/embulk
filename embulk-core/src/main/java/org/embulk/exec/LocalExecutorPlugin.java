package org.embulk.exec;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import com.google.inject.Inject;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.TaskReport;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecSession;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.ProcessTask;
import org.embulk.spi.ProcessState;
import org.embulk.spi.Schema;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.AbortTransactionResource;
import org.embulk.spi.CloseResource;
import org.embulk.spi.TransactionalPageOutput;
import org.embulk.plugin.compat.PluginWrappers;
import org.embulk.spi.util.Filters;
import org.embulk.spi.util.Executors;
import org.embulk.spi.util.Executors.ProcessStateCallback;

public class LocalExecutorPlugin
        implements ExecutorPlugin
{
    private int defaultMaxThreads;
    private int defaultMinThreads;

    @Inject
    public LocalExecutorPlugin(@ForSystemConfig ConfigSource systemConfig)
    {
        int cores = Runtime.getRuntime().availableProcessors();
        this.defaultMaxThreads = systemConfig.get(Integer.class, "max_threads", cores * 2);
        this.defaultMinThreads = systemConfig.get(Integer.class, "min_output_tasks", cores);
    }

    @Override
    public void transaction(ConfigSource config, Schema outputSchema, int inputTaskCount,
            ExecutorPlugin.Control control)
    {
        try (AbstractLocalExecutor exec = newExecutor(config, inputTaskCount)) {
            control.transaction(outputSchema, exec.getOutputTaskCount(), exec);
        }
    }

    private AbstractLocalExecutor newExecutor(ConfigSource config, int inputTaskCount)
    {
        Logger log = Exec.getLogger(LocalExecutorPlugin.class);
        int maxThreads = config.get(Integer.class, "max_threads", defaultMaxThreads);
        int minThreads = config.get(Integer.class, "min_output_tasks", defaultMinThreads);
        if (inputTaskCount > 0 && inputTaskCount < minThreads) {
            int scatterCount = (minThreads + inputTaskCount - 1) / inputTaskCount;
            log.info("Using local thread executor with max_threads={} / output tasks {} = input tasks {} * {}",
                    maxThreads, inputTaskCount * scatterCount, inputTaskCount, scatterCount);
            return new ScatterExecutor(maxThreads, inputTaskCount, scatterCount);
        }
        else {
            log.info("Using local thread executor with max_threads={} / tasks={}", maxThreads, inputTaskCount);
            return new DirectExecutor(maxThreads, inputTaskCount);
        }
    }

    private static abstract class AbstractLocalExecutor
            implements Executor, AutoCloseable
    {
        protected final Logger log = Exec.getLogger(LocalExecutorPlugin.class);

        protected final int inputTaskCount;
        protected final int outputTaskCount;

        public AbstractLocalExecutor(int inputTaskCount, int outputTaskCount)
        {
            this.inputTaskCount = inputTaskCount;
            this.outputTaskCount = outputTaskCount;
        }

        public int getOutputTaskCount()
        {
            return outputTaskCount;
        }

        @Override
        public void execute(ProcessTask task, ProcessState state)
        {
            state.initialize(inputTaskCount, outputTaskCount);

            List<Future<Throwable>> futures = new ArrayList<>(inputTaskCount);
            try {
                for (int i = 0; i < inputTaskCount; i++) {
                    futures.add(startInputTask(task, state, i));
                }
                showProgress(state, inputTaskCount);

                for (int i = 0; i < inputTaskCount; i++) {
                    if (futures.get(i) == null) {
                        continue;
                    }
                    try {
                        state.getInputTaskState(i).setException(futures.get(i).get());
                    }
                    catch (ExecutionException ex) {
                        state.getInputTaskState(i).setException(ex.getCause());
                        //Throwables.propagate(ex.getCause());
                    }
                    catch (InterruptedException ex) {
                        state.getInputTaskState(i).setException(new ExecutionInterruptedException(ex));
                    }
                    showProgress(state, inputTaskCount);
                }
            }
            finally {
                for (Future<Throwable> future : futures) {
                    if (future != null && !future.isDone()) {
                        future.cancel(true);
                        // TODO join?
                    }
                }
            }
        }

        @Override
        public abstract void close();

        private void showProgress(ProcessState state, int taskCount)
        {
            int started = 0;
            int finished = 0;
            for (int i = 0; i < taskCount; i++) {
                if (state.getOutputTaskState(i).isStarted()) { started++; }
                if (state.getOutputTaskState(i).isFinished()) { finished++; }
            }

            log.info(String.format("{done:%3d / %d, running: %d}", finished, taskCount, started - finished));
        }

        protected abstract Future<Throwable> startInputTask(ProcessTask task, ProcessState state, int taskIndex);
    }

    public static class DirectExecutor
            extends AbstractLocalExecutor
    {
        protected final ExecutorService executor;

        public DirectExecutor(int maxThreads, int taskCount)
        {
            super(taskCount, taskCount);
            this.executor = java.util.concurrent.Executors.newFixedThreadPool(maxThreads,
                    new ThreadFactoryBuilder()
                            .setNameFormat("embulk-executor-%d")
                            .setDaemon(true)
                            .build());
        }

        @Override
        public void close()
        {
            executor.shutdown();
        }

        @Override
        protected Future<Throwable> startInputTask(final ProcessTask task, final ProcessState state, final int taskIndex)
        {
            if (state.getOutputTaskState(taskIndex).isCommitted()) {
                log.warn("Skipped resumed task {}", taskIndex);
                return null;  // resumed
            }

            return executor.submit(new Callable<Throwable>() {
                public Throwable call()
                {
                    try (SetCurrentThreadName dontCare = new SetCurrentThreadName(String.format("task-%04d", taskIndex))) {
                        Executors.process(Exec.session(), task, taskIndex, new ProcessStateCallback() {
                            public void started()
                            {
                                state.getInputTaskState(taskIndex).start();
                                state.getOutputTaskState(taskIndex).start();
                            }

                            public void inputCommitted(TaskReport report)
                            {
                                state.getInputTaskState(taskIndex).setTaskReport(report);
                            }

                            public void outputCommitted(TaskReport report)
                            {
                                state.getOutputTaskState(taskIndex).setTaskReport(report);
                            }
                        });
                        return null;
                    }
                    finally {
                        state.getInputTaskState(taskIndex).finish();
                        state.getOutputTaskState(taskIndex).finish();
                    }
                }
            });
        }
    }

    public static class ScatterExecutor
            extends AbstractLocalExecutor
    {
        private final int scatterCount;
        private final int inputTaskCount;
        private final ExecutorService inputExecutor;
        private final ExecutorService outputExecutor;

        public ScatterExecutor(int maxThreads, int inputTaskCount, int scatterCount)
        {
            super(inputTaskCount, inputTaskCount * scatterCount);
            this.inputTaskCount = inputTaskCount;
            this.scatterCount = scatterCount;
            this.inputExecutor = java.util.concurrent.Executors.newFixedThreadPool(
                    Math.max(maxThreads / scatterCount, 1),
                    new ThreadFactoryBuilder()
                            .setNameFormat("embulk-input-executor-%d")
                            .setDaemon(true)
                            .build());
            this.outputExecutor = java.util.concurrent.Executors.newCachedThreadPool(
                    new ThreadFactoryBuilder()
                            .setNameFormat("embulk-output-executor-%d")
                            .setDaemon(true)
                            .build());
        }

        @Override
        public void close()
        {
            inputExecutor.shutdown();
            outputExecutor.shutdown();
        }

        @Override
        protected Future<Throwable> startInputTask(final ProcessTask task, final ProcessState state, final int taskIndex)
        {
            if(isAllScatterOutputFinished(state, taskIndex)) {
                log.warn("Skipped resumed input task {}", taskIndex);
                return null;  // resumed
            }

            return inputExecutor.submit(new Callable<Throwable>() {
                public Throwable call()
                {
                    try (SetCurrentThreadName dontCare = new SetCurrentThreadName(String.format("task-%04d", taskIndex))) {
                        runInputTask(Exec.session(), task, state, taskIndex);
                        return null;
                    }
                }
            });
        }

        private boolean isAllScatterOutputFinished(ProcessState state, int taskIndex) {
            for (int i = 0; i < scatterCount; i++) {
                int outputTaskIndex = taskIndex * scatterCount + i;
                if (!state.getOutputTaskState(outputTaskIndex).isCommitted()) {
                    return false;
                }
            }
            return true;
        }

        private void runInputTask(ExecSession exec, ProcessTask task, ProcessState state, int taskIndex)
        {
            InputPlugin inputPlugin = exec.newPlugin(InputPlugin.class, task.getInputPluginType());
            List<FilterPlugin> filterPlugins = Filters.newFilterPlugins(exec, task.getFilterPluginTypes());
            OutputPlugin outputPlugin = exec.newPlugin(OutputPlugin.class, task.getOutputPluginType());

            try (ScatterTransactionalPageOutput tran = new ScatterTransactionalPageOutput(state, taskIndex, scatterCount)) {
                tran.openOutputs(outputPlugin, task.getOutputSchema(), task.getOutputTaskSource());

                try (AbortTransactionResource aborter = new AbortTransactionResource(tran)) {
                    tran.openFilters(filterPlugins, task.getFilterSchemas(), task.getFilterTaskSources());

                    tran.startWorkers(outputExecutor);

                    // started
                    state.getInputTaskState(taskIndex).start();
                    for (int i = 0; i < scatterCount; i++) {
                        state.getOutputTaskState(taskIndex * scatterCount + i).start();
                    }

                    TaskReport inputTaskReport = inputPlugin.run(task.getInputTaskSource(), task.getInputSchema(), taskIndex, tran);

                    // inputCommitted
                    if (inputTaskReport == null) {
                        inputTaskReport = exec.newTaskReport();
                    }
                    state.getInputTaskState(taskIndex).setTaskReport(inputTaskReport);

                    // outputCommitted
                    tran.commit();
                    aborter.dontAbort();
                }
            }
            finally {
                state.getInputTaskState(taskIndex).finish();
                state.getOutputTaskState(taskIndex).finish();
            }
        }
    }

    private static class ScatterTransactionalPageOutput
            implements TransactionalPageOutput
    {
        private static final Page DONE_PAGE = Page.allocate(0);

        private static class OutputWorker
                implements Callable<Throwable>
        {
            private final PageOutput output;
            private final Future<Throwable> future;
            private volatile int addWaiting;
            private volatile Page queued;

            public OutputWorker(PageOutput output, ExecutorService executor)
            {
                this.output = output;
                this.addWaiting = 0;
                this.future = executor.submit(this);
            }

            public synchronized void done()
                    throws InterruptedException
            {
                while (true) {
                    if (queued == null && addWaiting == 0) {
                        queued = DONE_PAGE;
                        notifyAll();
                        return;
                    }
                    else if (queued == DONE_PAGE) {
                        return;
                    }
                    wait();
                }
            }

            public synchronized void add(Page page)
                    throws InterruptedException
            {
                addWaiting++;
                try {
                    while (true) {
                        if (queued == null) {
                            queued = page;
                            notifyAll();
                            return;
                        }
                        else if (queued == DONE_PAGE) {
                            page.release();
                            return;
                        }
                        wait();
                    }
                }
                finally {
                    addWaiting--;
                }
            }

            public Throwable join()
                    throws InterruptedException
            {
                try {
                    return future.get();
                }
                catch (ExecutionException ex) {
                    return ex.getCause();
                }
            }

            @Override
            public synchronized Throwable call()
                    throws InterruptedException
            {
                try {
                    while (true) {
                        if (queued != null) {
                            if (queued == DONE_PAGE) {
                                return null;
                            }
                            output.add(queued);
                            queued = null;
                            notifyAll();
                        }
                        wait();
                    }
                }
                finally {
                    try {
                        if (queued != null && queued != DONE_PAGE) {
                            queued.release();
                            queued = null;
                        }
                    }
                    finally {
                        queued = DONE_PAGE;
                    }
                    notifyAll();
                }
            }
        }

        private final ProcessState state;
        private final int taskIndex;
        private final int scatterCount;

        private final TransactionalPageOutput[] trans;
        private final PageOutput[] filtereds;
        private final CloseResource[] closeThese;

        private final OutputWorker[] outputWorkers;

        private long pageCount;

        public ScatterTransactionalPageOutput(ProcessState state, int taskIndex, int scatterCount)
        {
            this.state = state;
            this.taskIndex = taskIndex;
            this.scatterCount = scatterCount;

            this.trans = new TransactionalPageOutput[scatterCount];
            this.filtereds = new PageOutput[scatterCount];
            this.closeThese = new CloseResource[scatterCount];
            for (int i = 0; i < scatterCount; i++) {
                closeThese[i] = new CloseResource();
            }
            this.outputWorkers = new OutputWorker[scatterCount];
        }

        public void openOutputs(OutputPlugin outputPlugin, Schema outputSchema, TaskSource outputTaskSource)
        {
            for (int i = 0; i < scatterCount; i++) {
                int outputTaskIndex = taskIndex * scatterCount + i;
                if (!state.getOutputTaskState(outputTaskIndex).isCommitted()) {
                    TransactionalPageOutput tran = PluginWrappers.transactionalPageOutput(
                        outputPlugin.open(outputTaskSource, outputSchema, outputTaskIndex));
                    trans[i] = tran;
                    closeThese[i].closeThis(tran);
                }
            }
        }

        public void openFilters(List<FilterPlugin> filterPlugins, List<Schema> filterSchemas, List<TaskSource> filterTaskSources)
        {
            for (int i = 0; i < scatterCount; i++) {
                TransactionalPageOutput tran = trans[i];
                if (tran != null) {
                    PageOutput filtered = Filters.open(filterPlugins, filterTaskSources, filterSchemas, trans[i]);
                    filtereds[i] = filtered;
                    closeThese[i].closeThis(filtered);
                }
            }
        }

        public void startWorkers(ExecutorService outputExecutor)
        {
            for (int i = 0; i < scatterCount; i++) {
                PageOutput filtered = filtereds[i];
                if (filtered != null) {
                    outputWorkers[i] = new OutputWorker(filtered, outputExecutor);
                }
            }
        }

        public void add(Page page)
        {
            OutputWorker worker = outputWorkers[(int) (pageCount % scatterCount)];
            if (worker != null) {
                try {
                    worker.add(page);
                }
                catch (InterruptedException ex) {
                    throw Throwables.propagate(ex);
                }
            }
            pageCount++;
        }

        public void finish()
        {
            completeWorkers();
            for (int i = 0; i < scatterCount; i++) {
                if (filtereds[i] != null) {
                    filtereds[i].finish();
                }
            }
        }

        public void close()
        {
            completeWorkers();
            for (int i = 0; i < scatterCount; i++) {
                closeThese[i].close();
            }
        }

        public void abort()
        {
            completeWorkers();
            for (int i = 0; i < scatterCount; i++) {
                if (trans[i] != null) {
                    trans[i].abort();
                }
            }
        }

        public TaskReport commit()
        {
            completeWorkers();
            for (int i = 0; i < scatterCount; i++) {
                if (trans[i] != null) {
                    int outputTaskIndex = taskIndex * scatterCount + i;
                    TaskReport outputTaskReport = trans[i].commit();
                    trans[i] = null;  // don't abort
                    if (outputTaskReport == null) {
                        outputTaskReport = Exec.newTaskReport();
                    }
                    state.getOutputTaskState(outputTaskIndex).setTaskReport(outputTaskReport);
                }
            }
            return null;
        }

        public void completeWorkers()
        {
            for (int i = 0; i < scatterCount; i++) {
                OutputWorker worker = outputWorkers[i];
                if (worker != null) {
                    try {
                        worker.done();
                    }
                    catch (InterruptedException ex) {
                        throw Throwables.propagate(ex);
                    }
                    Throwable error = null;
                    try {
                        error = worker.join();
                    }
                    catch (InterruptedException ex) {
                        error = ex;
                    }
                    outputWorkers[i] = null;
                    if (error != null) {
                        throw Throwables.propagate(error);
                    }
                }
            }
        }
    }
}
