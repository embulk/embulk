package org.embulk.exec;

import com.google.common.base.Optional;
import java.util.List;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Schema;

public class ResumeState {
    private final ConfigSource execSessionConfigSource;
    private final TaskSource inputTaskSource;
    private final TaskSource outputTaskSource;
    private final Schema inputSchema;
    private final Schema outputSchema;
    private final List<Optional<TaskReport>> inputTaskReports;
    private final List<Optional<TaskReport>> outputTaskReports;

    public ResumeState(
            final ConfigSource execSessionConfigSource,
            final TaskSource inputTaskSource,
            final TaskSource outputTaskSource,
            final Schema inputSchema,
            final Schema outputSchema,
            final List<Optional<TaskReport>> inputTaskReports,
            final List<Optional<TaskReport>> outputTaskReports) {
        this.execSessionConfigSource = execSessionConfigSource;
        this.inputTaskSource = inputTaskSource;
        this.outputTaskSource = outputTaskSource;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.inputTaskReports = inputTaskReports;
        this.outputTaskReports = outputTaskReports;
    }

    public ConfigSource getExecSessionConfigSource() {
        return execSessionConfigSource;
    }

    public TaskSource getInputTaskSource() {
        return inputTaskSource;
    }

    public TaskSource getOutputTaskSource() {
        return outputTaskSource;
    }

    public Schema getInputSchema() {
        return inputSchema;
    }

    public Schema getOutputSchema() {
        return outputSchema;
    }

    public List<Optional<TaskReport>> getInputTaskReports() {
        return inputTaskReports;
    }

    public List<Optional<TaskReport>> getOutputTaskReports() {
        return outputTaskReports;
    }
}
