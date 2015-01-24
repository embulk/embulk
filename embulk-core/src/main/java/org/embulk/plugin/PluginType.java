package org.embulk.plugin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class PluginType
{
    private final String name;

    // TODO accept isObject()/ObjectNode for complex PluginSource
    @JsonCreator
    public PluginType(String name)
    {
        if (name == null) {
            throw new NullPointerException("name must not be null");
        }
        this.name = name;
    }

    @JsonValue
    public String getName()
    {
        return name;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof PluginType)) {
            return false;
        }
        PluginType o = (PluginType) other;
        return name.equals(o.name);
    }

    @Override
    public String toString()
    {
        return name;
    }
}
