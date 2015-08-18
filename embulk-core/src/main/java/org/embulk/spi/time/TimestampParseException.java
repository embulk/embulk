package org.embulk.spi.time;

import org.embulk.config.UserDataException;

public class TimestampParseException
        extends Exception
        implements UserDataException
{
    public TimestampParseException(String message)
    {
        super(message);
    }
}
