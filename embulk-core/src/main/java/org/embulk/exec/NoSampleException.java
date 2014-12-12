package org.embulk.exec;

public class NoSampleException
        extends RuntimeException
{
    public NoSampleException(String message)
    {
        super(message);
    }
}
