package org.embulk.exec;

import java.util.List;
import org.embulk.config.ConfigDiff;
import org.embulk.config.TaskReport;

public class ExecutionResult {
    private  ConfigDiff configDiff;
    private  boolean skipped;
    private  List<Throwable> ignoredExceptions;
    private  List<TaskReport> inputTaskReports;
    private  List<TaskReport> outputTaskReports;

    public ExecutionResult(ConfigDiff configDiff,  boolean skipped,  List<Throwable> ignoredExceptions,
                            List<TaskReport> inputTaskReports,  List<TaskReport> outputTaskReports) {
        this.configDiff = configDiff;
        this.skipped = skipped;
        this.ignoredExceptions = ignoredExceptions;
        this.inputTaskReports = inputTaskReports;
        this.outputTaskReports = outputTaskReports;
    }

    public ConfigDiff getConfigDiff() {
        return configDiff;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public List<Throwable> getIgnoredExceptions() {
        return ignoredExceptions;
    }

    public List<TaskReport> getOutputTaskReports() {
        return outputTaskReports;
    }

    public List<TaskReport> getInputTaskReports() {
        return inputTaskReports;
    }
}
