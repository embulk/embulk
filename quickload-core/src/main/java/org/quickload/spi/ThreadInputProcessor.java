package org.quickload.spi;

public abstract class ThreadInputProcessor
        implements InputProcessor, Runnable
{
    protected final Thread thread;
    protected final OutputOperator op;
    protected Report report;

    public static ThreadInputProcessor start(OutputOperator op, final Function<OutputOperator, ReportBuilder> body)
    {
        return new ThreadInputProcessor(op) {
            public ReportBuilder runThread()
            {
                return body.apply(op);
            }
        }.start();
    }

    public ThreadInputProcessor(OutputOperator op)
    {
        this.op = op;
        this.thread = new Thread(this);
    }

    public abstract ReportBuilder runThread() throws Exception;

    @Override
    public void run()
    {
        try {
            ReportBuilder reportBuilder = runThread();
            this.report = reportBuilder.build(op.completed());
        } catch (Exception ex) {
            this.report = new FailedReport(ex, op.failed(ex));
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
        op.close();
    }
}
