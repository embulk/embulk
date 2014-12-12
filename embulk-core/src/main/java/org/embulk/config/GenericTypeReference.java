package org.embulk.config;

import java.lang.reflect.Type;
import com.fasterxml.jackson.core.type.TypeReference;

class GenericTypeReference
        extends TypeReference<Object>
{
    private final Type type;

    public GenericTypeReference(Type type)
    {
        this.type = type;
    }

    public Type getType()
    {
        return type;
    }
}
