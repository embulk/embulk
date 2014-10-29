package org.quickload.channel;

public class ChannelInterruptedException
        extends RuntimeException
{
    public ChannelInterruptedException()
    {
    }

    public ChannelInterruptedException(Throwable cause)
    {
        super(cause);
    }
}
