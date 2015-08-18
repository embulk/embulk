package org.embulk.spi;

import org.embulk.config.UserDataException;

public class DataException
        extends RuntimeException
        implements UserDataException
{
    public DataException(String message)
    {
        super(message);
    }

    public DataException(Throwable cause)
    {
        super(cause);
    }

    public DataException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
