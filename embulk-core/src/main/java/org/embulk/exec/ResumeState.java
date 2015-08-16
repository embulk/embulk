package org.embulk.exec;

import java.util.List;
import com.google.common.base.Optional;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.CommitReport;
import org.embulk.spi.Schema;

public class ResumeState
{
    private final ConfigSource execSessionConfigSource;
    private final TaskSource inputTaskSource;
    private final TaskSource outputTaskSource;
    private final Schema inputSchema;
    private final Schema outputSchema;
    private final List<Optional<TaskReport>> inputTaskReports;
    private final List<Optional<TaskReport>> outputTaskReports;

    @JsonCreator
    public ResumeState(
            @JsonProperty("exec_task") ConfigSource execSessionConfigSource,
            @JsonProperty("in_task") TaskSource inputTaskSource,
            @JsonProperty("out_task") TaskSource outputTaskSource,
            @JsonProperty("in_schema") Schema inputSchema,
            @JsonProperty("out_schema") Schema outputSchema,
            @JsonProperty("in_reports") List<Optional<TaskReport>> inputTaskReports,
            @JsonProperty("out_reports") List<Optional<TaskReport>> outputTaskReports)
    {
        this.execSessionConfigSource = execSessionConfigSource;
        this.inputTaskSource = inputTaskSource;
        this.outputTaskSource = outputTaskSource;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.inputTaskReports = inputTaskReports;
        this.outputTaskReports = outputTaskReports;
    }

    @JsonProperty("exec_task")
    public ConfigSource getExecSessionConfigSource()
    {
        return execSessionConfigSource;
    }

    @JsonProperty("in_task")
    public TaskSource getInputTaskSource()
    {
        return inputTaskSource;
    }

    @JsonProperty("out_task")
    public TaskSource getOutputTaskSource()
    {
        return outputTaskSource;
    }

    @JsonProperty("in_schema")
    public Schema getInputSchema()
    {
        return inputSchema;
    }

    @JsonProperty("out_schema")
    public Schema getOutputSchema()
    {
        return outputSchema;
    }

    @JsonProperty("in_reports")
    public List<Optional<TaskReport>> getInputTaskReports()
    {
        return inputTaskReports;
    }

    @Deprecated
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public List<Optional<CommitReport>> getInputCommitReports()
    {
        return (List) inputTaskReports;  // the only implementation of TaskReport is DataSourceImpl which implements CommitReport
    }

    @JsonProperty("out_reports")
    public List<Optional<TaskReport>> getOutputTaskReports()
    {
        return outputTaskReports;
    }

    @Deprecated
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public List<Optional<CommitReport>> getOutputCommitReports()
    {
        return (List) outputTaskReports;  // the only implementation of TaskReport is DataSourceImpl which implements CommitReport;
    }
}
