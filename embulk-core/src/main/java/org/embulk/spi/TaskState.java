package org.embulk.spi;

import java.util.Map;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.embulk.spi.MixinId;
import org.embulk.config.TaskReport;
import org.embulk.config.CommitReport;

public class TaskState
{
    private volatile boolean started = false;
    private volatile boolean finished = false;
    private volatile Optional<TaskReport> taskReport = Optional.absent();
    private volatile Map<MixinId, TaskReport> mixinReports = ImmutableMap.of();
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

    public void setTaskReport(TaskReport taskReport)
    {
        this.started = true;
        this.taskReport = Optional.of(taskReport);
    }

    @Deprecated
    public void setCommitReport(CommitReport commitReport)
    {
        this.started = true;
        this.taskReport = Optional.<TaskReport>of(commitReport);
    }

    public void setMixinReports(Map<MixinId, TaskReport> mixinReports)
    {
        this.mixinReports = mixinReports;
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
        return taskReport.isPresent();
    }

    public Optional<TaskReport> getTaskReport()
    {
        return taskReport;
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public Optional<CommitReport> getCommitReport()
    {
        return (Optional) taskReport;  // the only implementation of TaskReport is DataSourceImpl which implements CommitReport;
    }

    public Map<MixinId, TaskReport> getMixinReports()
    {
        return mixinReports;
    }

    public Optional<Throwable> getException()
    {
        return exception;
    }
}
