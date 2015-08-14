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
import org.embulk.config.TaskReport;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.ProcessTask;
import org.embulk.spi.ProcessState;
import org.embulk.spi.TaskState;
import org.embulk.spi.Schema;
import org.embulk.spi.util.Executors;
import org.embulk.spi.util.Executors.ProcessStateCallback;

public class LocalExecutorPlugin
        implements ExecutorPlugin
{
    private final ExecutorService executor;

    @Inject
    public LocalExecutorPlugin(LocalThreadExecutor executor)
    {
        this.executor = executor.getExecutorService();
    }

    @Override
    public void transaction(ConfigSource config, Schema outputSchema, final int inputTaskCount,
            ExecutorPlugin.Control control)
    {
        control.transaction(outputSchema, inputTaskCount, new Executor() {
            public void execute(ProcessTask task, ProcessState state)
            {
                localExecute(task, inputTaskCount, state);
            }
        });
    }

    private void localExecute(ProcessTask task, int taskCount, ProcessState state)
    {
        Logger log = Exec.getLogger(LocalExecutorPlugin.class);

        state.initialize(taskCount, taskCount);

        List<Future<Throwable>> futures = new ArrayList<>(taskCount);
        try {
            for (int i=0; i < taskCount; i++) {
                if (state.getOutputTaskState(i).isCommitted()) {
                    log.warn("Skipped resumed task {}", i);
                    futures.add(null);  // resumed
                } else {
                    futures.add(startProcessor(task, i, state));
                }
            }
            showProgress(log, state, taskCount);

            for (int i=0; i < taskCount; i++) {
                if (futures.get(i) == null) {
                    continue;
                }
                try {
                    state.getInputTaskState(i).setException(futures.get(i).get());
                } catch (ExecutionException ex) {
                    state.getInputTaskState(i).setException(ex.getCause());
                    //Throwables.propagate(ex.getCause());
                } catch (InterruptedException ex) {
                    state.getInputTaskState(i).setException(new ExecutionInterruptedException(ex));
                }
                showProgress(log, state, taskCount);
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

    private void showProgress(Logger log, ProcessState state, int taskCount)
    {
        int started = 0;
        int finished = 0;
        for (int i=0; i < taskCount; i++) {
            if (state.getInputTaskState(i).isStarted()) { started++; }
            if (state.getOutputTaskState(i).isFinished()) { finished++; }
        }

        log.info(String.format("{done:%3d / %d, running: %d}", finished, taskCount, started - finished));
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
                } finally {
                    state.getInputTaskState(taskIndex).finish();
                    state.getOutputTaskState(taskIndex).finish();
                }
            }
        });
    }
}
