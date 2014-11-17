package org.quickload.spi;

import java.util.List;
import java.util.Deque;
import java.util.ArrayDeque;
//import javax.annotation.Nullable;  // TODO jsr305
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

public class PluginThread
{
    public static PluginThread start(final Runnable runnable)
    {
        return new PluginThread(runnable);
    }

    private static class Runner
            implements Runnable
    {
        private final Runnable runnable;
        private RuntimeException runtimeException;
        private Error error;
        private Throwable throwable;

        public Runner(Runnable runnable)
        {
            this.runnable = runnable;
        }

        public void run()
        {
            try {
                runnable.run();
            } catch (RuntimeException runtimeException) {
                this.runtimeException = runtimeException;
            } catch (Error error) {
                this.error = error;
            } catch (Throwable throwable) {
                this.throwable = throwable;
            }
        }

        public void throwException()
        {
            if (runtimeException != null) {
                throw runtimeException;
            } else if (error != null) {
                throw error;
            } else if (throwable != null) {
                throw new PluginExecutionException(throwable);
            }
        }
    }

    private final Runner runner;
    private final Thread thread;

    private PluginThread(Runnable runnable)
    {
        this.runner = new Runner(runnable);
        this.thread = new Thread(runner);
        thread.start();
    }

    public void join()
    {
        try {
            thread.join();
        } catch (InterruptedException ex) {
            throw new PluginInterruptedException(ex);
        }
    }

    public void joinAndThrow()
    {
        join();
        runner.throwException();
    }

    public static void joinAndThrowNested(/*@Nullable*/ PluginThread nestedThread)
    {
        joinAndThrowNested(nestedThread, null);
    }

    public static void joinAndThrowNested(/*@Nullable*/ PluginThread nestedThread, /*@Nullable*/ Throwable ex)
    {
        if (nestedThread == null) {
            if (ex == null) {
                return;
            }
            throw Throwables.propagate(ex);
        }
        joinAndThrowNested(ImmutableList.of(nestedThread), ex);
    }

    public static void joinAndThrowNested(List<PluginThread> nestedThreads)
    {
        joinAndThrowNested(nestedThreads, null);
    }

    public static void joinAndThrowNested(List<PluginThread> nestedThreads, /*@Nullable*/ Throwable ex)
    {
        if (nestedThreads.isEmpty()) {
            if (ex == null) {
                return;
            }
            throw Throwables.propagate(ex);
        }

        Deque<Throwable> nestedExceptions = new ArrayDeque();
        for (PluginThread thread : nestedThreads) {
            try {
                thread.joinAndThrow();
            } catch (Throwable nestedException) {
                nestedExceptions.addLast(nestedException);
            }
        }

        Throwable representative;
        if (ex != null) {
            representative = ex;
        } else {
            if (nestedExceptions.isEmpty()) {
                return;
            }
            representative = nestedExceptions.removeFirst();
        }
        for (Throwable suppressed : nestedExceptions) {
            representative.addSuppressed(suppressed);
        }
        throw Throwables.propagate(representative);
    }
}
