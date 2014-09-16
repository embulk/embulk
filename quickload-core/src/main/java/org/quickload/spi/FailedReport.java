package org.quickload.spi;

public class FailedReport
        implements Report
{
    private final Exception cause;
    private Report nextReport;

    public FailedReport(Exception cause, Report nextReport)
    {
        this.cause = cause;
        this.nextReport = nextReport;
    }

    public Exception getCause()
    {
        return cause;
    }

    public Report getNextReport()
    {
        return nextReport;
    }
}
