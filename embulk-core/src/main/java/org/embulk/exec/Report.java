package org.embulk.exec;

import java.util.List;

import org.embulk.config.TaskReport;

public class Report {
    private final List<TaskReport> input;
    private final List<TaskReport> output;

    public Report(List<TaskReport> input, List<TaskReport> output) {
        this.input = input;
        this.output = output;
    }

    public List<TaskReport> getInput() {
        return input;
    }

    public List<TaskReport> getOutput() {
        return output;
    }
}