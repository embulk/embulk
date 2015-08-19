package org.embulk.config;

public class ConfigException
        extends RuntimeException
        implements UserDataException
{
    public ConfigException(String message)
    {
        super(message);
    }

    public ConfigException(Throwable cause)
    {
        super(cause);
    }

    public ConfigException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
