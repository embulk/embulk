package org.embulk.spi;

import org.embulk.config.ConfigException;

public class SchemaConfigException
        extends ConfigException
{
    public SchemaConfigException(String message)
    {
        super(message);
    }

    public SchemaConfigException(Throwable cause)
    {
        super(cause);
    }

    public SchemaConfigException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
