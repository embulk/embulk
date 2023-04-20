package org.embulk.exec;

import java.util.ArrayList;
import java.util.List;
import org.embulk.config.ConfigDiff;
import org.embulk.config.TaskReport;

public class ExecutionResult {
    private final ConfigDiff configDiff;
    private final boolean skipped;
    private final List<Throwable> ignoredExceptions;
    private List<TaskReport> inputTaskReports = new ArrayList<>();
    private List<TaskReport> outputTaskReports = new ArrayList<>();

    public ExecutionResult(ConfigDiff configDiff, boolean skipped, List<Throwable> ignoredExceptions,
                           List<TaskReport> inputTaskReports, List<TaskReport> outputTaskReports) {
        this(configDiff, skipped, ignoredExceptions);
        this.inputTaskReports = inputTaskReports;
        this.outputTaskReports = outputTaskReports;
    }

    public ExecutionResult(ConfigDiff configDiff, boolean skipped, List<Throwable> ignoredExceptions) {
        this.configDiff = configDiff;
        this.skipped = skipped;
        this.ignoredExceptions = ignoredExceptions;
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
