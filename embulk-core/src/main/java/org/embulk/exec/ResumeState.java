package org.embulk.exec;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.CommitReport;
import org.embulk.spi.Schema;
import org.embulk.spi.ExecSession;

public class ResumeState
{
    private final ConfigSource execSessionConfigSource;
    private final TaskSource inputTaskSource;
    private final TaskSource outputTaskSource;
    private final Schema inputSchema;
    private final Schema outputSchema;
    private final int processorCount;
    private final List<CommitReport> inputCommitReports;
    private final List<CommitReport> outputCommitReports;

    @JsonCreator
    public ResumeState(
            @JsonProperty("exec_task") ConfigSource execSessionConfigSource,
            @JsonProperty("in_task") TaskSource inputTaskSource,
            @JsonProperty("out_task") TaskSource outputTaskSource,
            @JsonProperty("in_schema") Schema inputSchema,
            @JsonProperty("out_schema") Schema outputSchema,
            @JsonProperty("processors") int processorCount,
            @JsonProperty("in_reports") List<CommitReport> inputCommitReports,
            @JsonProperty("out_reports") List<CommitReport> outputCommitReports)
    {
        this.execSessionConfigSource = execSessionConfigSource;
        this.inputTaskSource = inputTaskSource;
        this.outputTaskSource = outputTaskSource;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.processorCount = processorCount;
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

    @JsonProperty("processors")
    public int getProcessrCount()
    {
        return processorCount;
    }

    @JsonProperty("in_reports")
    public List<CommitReport> getInputCommitReports()
    {
        return inputCommitReports;
    }

    @JsonProperty("out_reports")
    public List<CommitReport> getOutputCommitReports()
    {
        return outputCommitReports;
    }
}
