package org.embulk.spi;

import java.util.Optional;
import org.embulk.config.TaskReport;

public class TaskState {
    private volatile boolean started = false;
    private volatile boolean finished = false;
    private volatile Optional<TaskReport> taskReport = Optional.empty();
    private volatile Optional<Throwable> exception = Optional.empty();

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

    public void setException(Throwable exception) {
        this.started = true;
        this.exception = Optional.ofNullable(exception);
    }

    public void resetException() {
        this.started = true;
        this.exception = Optional.empty();
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

    public Optional<Throwable> getException() {
        return exception;
    }
}
