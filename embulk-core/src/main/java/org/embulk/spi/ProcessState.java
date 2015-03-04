package org.embulk.spi;

import org.embulk.config.CommitReport;

public interface ProcessState
{
    public void start(int taskIndex);

    public void finish(int taskIndex);

    public boolean isStarted(int taskIndex);

    public boolean isFinished(int taskIndex);

    public void setInputCommitReport(int taskIndex, CommitReport inputCommitReport);

    public void setOutputCommitReport(int taskIndex, CommitReport outputCommitReport);

    public boolean isOutputCommitted(int taskIndex);

    public void setException(int taskIndex, Throwable exception);

    public boolean isExceptionSet(int taskIndex);

    // other getter methods
}
