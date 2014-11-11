package org.quickload.spi;

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
