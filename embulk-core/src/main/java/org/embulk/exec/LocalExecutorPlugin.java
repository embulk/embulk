package org.embulk.exec;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import com.google.inject.Inject;
import org.embulk.config.ConfigSource;
import org.embulk.config.CommitReport;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.ProcessState;
import org.embulk.spi.ProcessTask;
import org.embulk.spi.util.Executors;
import org.embulk.spi.util.Executors.ProcessStateCallback;
import static org.embulk.spi.util.Executors.getStartedCount;
import static org.embulk.spi.util.Executors.getFinishedCount;

public class LocalExecutorPlugin
        implements ExecutorPlugin
{
    private final ExecutorService executor;

    @Inject
    public LocalExecutorPlugin(LocalThreadExecutor executor)
    {
        this.executor = executor.getExecutorService();
    }

    public void transaction(ConfigSource config, ExecutorPlugin.Control control)
    {
        control.transaction(new Executor() {
            public void execute(ProcessTask task, int taskCount, ProcessState state)
            {
                localExecute(task, taskCount, state);
            }
        });
    }

    private void localExecute(ProcessTask task, int taskCount, ProcessState state)
    {
        Logger logger = Exec.getLogger(LocalExecutorPlugin.class);

        List<Future<Throwable>> futures = new ArrayList<>(taskCount);
        try {
            for (int i=0; i < taskCount; i++) {
                if (state.isOutputCommitted(i)) {
                    logger.warn("Skipped resumed task {}", i);
                    futures.add(null);  // resumed
                } else {
                    futures.add(startProcessor(task, i, state));
                }
            }
            showProgress(logger, state, taskCount);

            for (int i=0; i < taskCount; i++) {
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
                showProgress(logger, state, taskCount);
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

    private void showProgress(Logger logger, ProcessState state, int taskCount)
    {
        int finished = getStartedCount(state, taskCount);
        int started = getFinishedCount(state, taskCount);
        logger.info(String.format("{done:%3d / %d, running: %d}", finished, taskCount, started - finished));
    }

    private Future<Throwable> startProcessor(final ProcessTask task, final int taskIndex, final ProcessState state)
    {
        return executor.submit(new Callable<Throwable>() {
            public Throwable call()
            {
                try (SetCurrentThreadName dontCare = new SetCurrentThreadName(String.format("task-%04d", taskIndex))) {
                    Executors.process(Exec.session(), task, taskIndex, new ProcessStateCallback() {
                        public void started()
                        {
                            state.start(taskIndex);
                        }

                        public void inputCommitted(CommitReport report)
                        {
                            state.setInputCommitReport(taskIndex, report);
                        }

                        public void outputCommitted(CommitReport report)
                        {
                            state.setOutputCommitReport(taskIndex, report);
                        }

                        public void finished()
                        {
                            state.finish(taskIndex);
                        }
                    });
                    return null;
                }
            }
        });
    }
}
