package org.embulk.spi;

import com.google.common.base.Optional;
import org.embulk.config.CommitReport;

public class TaskState
{
    private volatile boolean started = false;
    private volatile boolean finished = false;
    private volatile Optional<CommitReport> commitReport = Optional.absent();
    private volatile Optional<Throwable> exception = Optional.absent();

    public void start()
    {
        this.started = true;
    }

    public void finish()
    {
        this.started = true;
        this.finished = true;
    }

    public void setCommitReport(CommitReport commitReport)
    {
        this.started = true;
        this.commitReport = Optional.of(commitReport);
    }

    public void setException(Throwable exception)
    {
        this.started = true;
        this.exception = Optional.fromNullable(exception);
    }

    public void resetException()
    {
        this.started = true;
        this.exception = Optional.absent();
    }

    public boolean isStarted()
    {
        return started;
    }

    public boolean isFinished()
    {
        return finished;
    }

    public boolean isCommitted()
    {
        return commitReport.isPresent();
    }

    public Optional<CommitReport> getCommitReport()
    {
        return commitReport;
    }

    public Optional<Throwable> getException()
    {
        return exception;
    }
}
