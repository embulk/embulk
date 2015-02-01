package org.embulk.exec;

public class PartialExecuteException
        extends RuntimeException
{
    private final ResumeState resumeState;

    public PartialExecuteException(Throwable cause, ResumeState resumeState)
    {
        super(cause);
        this.resumeState = resumeState;
    }

    public ResumeState getResumeState()
    {
        return resumeState;
    }
}
