package org.embulk.exec;

import java.util.List;
import com.google.common.base.Optional;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.CommitReport;
import org.embulk.spi.Schema;

public class ResumeState
{
    private final ConfigSource execSessionConfigSource;
    private final TaskSource inputTaskSource;
    private final TaskSource outputTaskSource;
    private final Schema inputSchema;
    private final Schema outputSchema;
    private final List<Optional<CommitReport>> inputCommitReports;
    private final List<Optional<CommitReport>> outputCommitReports;

    @JsonCreator
    public ResumeState(
            @JsonProperty("exec_task") ConfigSource execSessionConfigSource,
            @JsonProperty("in_task") TaskSource inputTaskSource,
            @JsonProperty("out_task") TaskSource outputTaskSource,
            @JsonProperty("in_schema") Schema inputSchema,
            @JsonProperty("out_schema") Schema outputSchema,
            @JsonProperty("in_reports") List<Optional<CommitReport>> inputCommitReports,
            @JsonProperty("out_reports") List<Optional<CommitReport>> outputCommitReports)
    {
        this.execSessionConfigSource = execSessionConfigSource;
        this.inputTaskSource = inputTaskSource;
        this.outputTaskSource = outputTaskSource;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.inputCommitReports = inputCommitReports;
        this.outputCommitReports = outputCommitReports;
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
    public List<Optional<CommitReport>> getInputCommitReports()
    {
        return inputCommitReports;
    }

    @JsonProperty("out_reports")
    public List<Optional<CommitReport>> getOutputCommitReports()
    {
        return outputCommitReports;
    }
}
