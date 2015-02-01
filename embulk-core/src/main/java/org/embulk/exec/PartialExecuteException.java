package org.embulk.exec;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.CommitReport;

public class PartialExecuteException
        extends RuntimeException
{
    private final TaskSource inputTask;
    private final TaskSource outputTask;
    private final List<CommitReport> inputCommitReports;
    private final List<CommitReport> outputCommitReports;

    public PartialExecuteException(Throwable cause,
            TaskSource inputTask, TaskSource outputTask,
            List<CommitReport> inputCommitReports, List<CommitReport> outputCommitReports)
    {
        super(cause);
        this.inputTask = inputTask;
        this.outputTask = outputTask;
        this.outputCommitReports = outputCommitReports;
        this.inputCommitReports = inputCommitReports;
    }
}
