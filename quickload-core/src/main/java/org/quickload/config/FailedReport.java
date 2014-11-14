package org.quickload.config;

public class FailedReport
        extends Report
{
    private final Throwable cause;

    public FailedReport(Throwable cause)
    {
        super();
        this.cause = cause;
    }

    public Throwable getCause()
    {
        return cause;
    }
}
