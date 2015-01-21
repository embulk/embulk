package org.embulk.exec;

public class ExecuteInterruptedException
        extends RuntimeException
{
    public ExecuteInterruptedException(Exception cause)
    {
        super(cause);
    }
}
