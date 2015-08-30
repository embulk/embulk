package org.embulk.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class MixinId
{
    private String id;

    @JsonCreator
    public MixinId(String id)
    {
        this.id = id;
    }

    @JsonValue
    public String getString()
    {
        return id;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof MixinId)) {
            return false;
        }
        MixinId obj = (MixinId) o;
        return id.equals(obj.id);
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public String toString()
    {
        return id;
    }
}
