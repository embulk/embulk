package org.embulk.spi.type;

import org.msgpack.value.Value;

public class JsonType
        extends AbstractType
{
    static final JsonType JSON = new JsonType();

    private JsonType()
    {
        super("json", Value.class, 4);
    }
}
