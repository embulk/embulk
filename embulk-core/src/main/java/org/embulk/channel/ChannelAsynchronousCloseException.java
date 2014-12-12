package org.embulk.channel;

public class ChannelAsynchronousCloseException
        extends RuntimeException
{
    public ChannelAsynchronousCloseException(String message)
    {
        super(message);
    }
}
