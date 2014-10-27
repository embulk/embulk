package org.quickload.config;

public class FailedReport
        implements Report
{
    private final Exception cause;

    public FailedReport(Exception cause)
    {
        this.cause = cause;
    }

    public Exception getCause()
    {
        return cause;
    }
}
