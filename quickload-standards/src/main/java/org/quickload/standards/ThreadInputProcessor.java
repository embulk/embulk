package org.quickload.standards;

import com.google.common.base.Function;
import org.quickload.spi.*;

public abstract class ThreadInputProcessor<T extends Operator>
        implements InputProcessor, Runnable
{
    protected final Thread thread;
    protected final T next;
    protected Report report;

    public static <T extends Operator> ThreadInputProcessor start(T next, final Function<T, ReportBuilder> body)
    {
        ThreadInputProcessor<T> proc = new ThreadInputProcessor<T>(next) {
            @Override
            public ReportBuilder runThread()
            {
                return body.apply(next);
            }

            @Override
            public InputProgress getProgress() {
                return null;
            }
        };
        proc.thread.start();
        return proc;
    }

    public ThreadInputProcessor(T next)
    {
        this.next = next;
        this.thread = new Thread(this);
    }

    public abstract ReportBuilder runThread() throws Exception;

    @Override
    public void run()
    {
        try {
            ReportBuilder reportBuilder = runThread();
            this.report = reportBuilder.build(next.completed());
        } catch (Exception ex) {
            this.report = new FailedReport(ex, next.failed(ex));
        }
    }

    @Override
    public void cancel()
    {
        thread.interrupt();
    }

    @Override
    public Report join() throws InterruptedException
    {
        thread.join();
        return report;
    }

    @Override
    public void close() throws Exception
    {
        next.close();
    }
}
