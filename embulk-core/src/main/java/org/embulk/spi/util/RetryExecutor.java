package org.embulk.spi.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class RetryExecutor
{
    public static RetryExecutor retryExecutor()
    {
        // TODO default configuration
        return new RetryExecutor(3, 500, 30*60*1000);
    }

    public static class RetryGiveupException
            extends ExecutionException
    {
        public RetryGiveupException(String message, Exception cause)
        {
            super(cause);
        }

        public RetryGiveupException(Exception cause)
        {
            super(cause);
        }

        public Exception getCause()
        {
            return (Exception) super.getCause();
        }
    }

    public static interface Retryable<T>
            extends Callable<T>
    {
        public T call()
            throws Exception;

        public boolean isRetryableException(Exception exception);

        public void onRetry(Exception exception, int retryCount, int retryLimit, int retryWait)
            throws RetryGiveupException;

        public void onGiveup(Exception firstException, Exception lastException)
            throws RetryGiveupException;
    }

    private final int retryLimit;
    private final int initialRetryWait;
    private final int maxRetryWait;

    private RetryExecutor(int retryLimit, int initialRetryWait, int maxRetryWait)
    {
        this.retryLimit = retryLimit;
        this.initialRetryWait = initialRetryWait;
        this.maxRetryWait = maxRetryWait;
    }

    public RetryExecutor withRetryLimit(int count)
    {
        return new RetryExecutor(count, initialRetryWait, maxRetryWait);
    }

    public RetryExecutor withInitialRetryWait(int msec)
    {
        return new RetryExecutor(retryLimit, msec, maxRetryWait);
    }

    public RetryExecutor withMaxRetryWait(int msec)
    {
        return new RetryExecutor(retryLimit, initialRetryWait, msec);
    }

    public <T> T runInterruptible(Retryable<T> op)
            throws InterruptedException, RetryGiveupException
    {
        return run(op, true);
    }

    public <T> T run(Retryable<T> op)
            throws RetryGiveupException
    {
        try {
            return run(op, false);
        } catch (InterruptedException ex) {
            throw new RetryGiveupException("Unexpected interruption", ex);
        }
    }

    private <T> T run(Retryable<T> op, boolean interruptible)
            throws InterruptedException, RetryGiveupException
    {
        int retryWait = initialRetryWait;
        int retryCount = 0;

        Exception firstException = null;

        while(true) {
            try {
                return op.call();
            } catch (Exception exception) {
                if (firstException == null) {
                    firstException = exception;
                }
                if (!op.isRetryableException(exception) || retryCount >= retryLimit) {
                    op.onGiveup(firstException, exception);
                    throw new RetryGiveupException(firstException);
                }

                retryCount++;
                op.onRetry(exception, retryCount, retryLimit, retryWait);

                try {
                    Thread.sleep(retryWait);
                } catch (InterruptedException ex) {
                    if (interruptible) {
                        throw ex;
                    }
                }

                // exponential back-off with hard limit
                retryWait *= 2;
                if (retryWait > maxRetryWait) {
                    retryWait = maxRetryWait;
                }
            }
        }
    }
}

