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
        this.name = name;
    }

    @JsonValue
    public String getName()
    {
        return name;
    }
}
