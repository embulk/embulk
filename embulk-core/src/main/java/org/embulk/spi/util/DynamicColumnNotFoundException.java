package org.embulk.spi.util;

public class DynamicColumnNotFoundException
        extends RuntimeException
{
    public DynamicColumnNotFoundException(String message)
    {
        super(message);
    }
}
