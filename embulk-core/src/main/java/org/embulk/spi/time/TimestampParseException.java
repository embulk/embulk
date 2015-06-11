package org.embulk.spi.time;

public class TimestampParseException
        extends Exception
{
    public TimestampParseException(String message)
    {
        super(message);
    }
}
