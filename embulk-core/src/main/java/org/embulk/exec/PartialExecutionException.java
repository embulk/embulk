package org.embulk.exec;

public class PartialExecutionException
        extends RuntimeException
{
    private final ResumeState resumeState;

    public PartialExecutionException(Throwable cause, ResumeState resumeState)
    {
        super(cause);
        this.resumeState = resumeState;
    }

    public ResumeState getResumeState()
    {
        return resumeState;
    }
}
