package org.embulk.plugin;

public class PluginSourceNotMatchException
        extends Exception
{
    public PluginSourceNotMatchException()
    {
    }

    public PluginSourceNotMatchException(String message)
    {
        super(message);
    }

    public PluginSourceNotMatchException(Throwable cause)
    {
        super(cause);
    }
}
