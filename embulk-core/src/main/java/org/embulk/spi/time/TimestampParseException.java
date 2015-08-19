package org.embulk.spi.time;

import org.embulk.spi.DataException;

public class TimestampParseException
        extends DataException
{
    public TimestampParseException(String message)
    {
        super(message);
    }
}
