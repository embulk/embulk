package org.embulk.exec;

public class ExecutionInterruptedException
        extends RuntimeException
{
    public ExecutionInterruptedException(Exception cause)
    {
        super(cause);
    }
}
