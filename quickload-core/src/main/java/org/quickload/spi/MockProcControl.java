package org.quickload.spi;

import java.util.List;
import com.google.common.collect.ImmutableList;
import org.quickload.config.TaskSource;
import org.quickload.config.Report;

public class MockProcControl
        implements ProcControl
{
    private final List<Report> reports;
    private TaskSource taskSource;

    public MockProcControl()
    {
        this(ImmutableList.of(new Report()));
    }

    public MockProcControl(List<Report> reports)
    {
        this.reports = reports;
    }

    public List<Report> getReports()
    {
        return reports;
    }

    public TaskSource getTaskSource()
    {
        return taskSource;
    }

    public List<Report> run(TaskSource taskSource)
    {
        this.taskSource = taskSource;
        return reports;
    }
}
