package org.embulk.spi;

public class PluginExecutionException
        extends RuntimeException
{
    public PluginExecutionException(Throwable cause)
    {
        super(cause);
    }
}
