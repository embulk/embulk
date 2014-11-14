package org.quickload.spi;

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
}
