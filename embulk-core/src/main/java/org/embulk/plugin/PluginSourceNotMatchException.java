package org.embulk.plugin;

public class PluginSourceNotMatchException
        extends Exception
{
    public PluginSourceNotMatchException()
    {
        super();
    }

    public PluginSourceNotMatchException(String message)
    {
        super(message);
    }

    public PluginSourceNotMatchException(Throwable cause)
    {
        super(cause);
    }

    public PluginSourceNotMatchException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
