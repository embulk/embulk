package org.embulk.spi.json;

import org.embulk.spi.DataException;

public class JsonParseException
        extends DataException
{
    public JsonParseException(String message)
    {
        super(message);
    }

    public JsonParseException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
