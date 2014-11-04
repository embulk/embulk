package org.quickload.config;

public class FailedReport
        extends Report
{
    private final Exception cause;

    public FailedReport(Exception cause)
    {
        super();
        this.cause = cause;
    }

    public Exception getCause()
    {
        return cause;
    }
}
