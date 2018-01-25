package org.embulk.spi;

import com.google.common.base.Optional;
import org.embulk.config.TaskReport;

public class TaskState {
    private volatile boolean started = false;
    private volatile boolean finished = false;
    private volatile Optional<TaskReport> taskReport = Optional.absent();
    private volatile Optional<Throwable> exception = Optional.absent();

    public void start() {
        this.started = true;
    }

    public void finish() {
        this.started = true;
        this.finished = true;
    }

    public void setTaskReport(TaskReport taskReport) {
        this.started = true;
        this.taskReport = Optional.of(taskReport);
    }

    // To be removed by v0.10 or earlier.
    @Deprecated  // https://github.com/embulk/embulk/issues/933
    @SuppressWarnings("deprecation")
    public void setCommitReport(org.embulk.config.CommitReport commitReport) {
        this.started = true;
        this.taskReport = Optional.<TaskReport>of(commitReport);
    }

    public void setException(Throwable exception) {
        this.started = true;
        this.exception = Optional.fromNullable(exception);
    }

    public void resetException() {
        this.started = true;
        this.exception = Optional.absent();
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isCommitted() {
        return taskReport.isPresent();
    }

    public Optional<TaskReport> getTaskReport() {
        return taskReport;
    }

    // To be removed by v0.10 or earlier.
    @Deprecated  // https://github.com/embulk/embulk/issues/933
    @SuppressWarnings({"deprecation", "unchecked"})
    public Optional<org.embulk.config.CommitReport> getCommitReport() {
        return (Optional) taskReport;  // the only implementation of TaskReport is DataSourceImpl which implements CommitReport;
    }

    public Optional<Throwable> getException() {
        return exception;
    }
}
