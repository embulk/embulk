package org.embulk.spi;

public class PluginInterruptedException
        extends RuntimeException
{
    public PluginInterruptedException()
    {
    }

    public PluginInterruptedException(Throwable cause)
    {
        super(cause);
    }
}
