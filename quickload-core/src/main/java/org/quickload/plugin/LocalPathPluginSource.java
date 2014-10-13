package org.quickload.plugin;

import com.fasterxml.jackson.databind.JsonNode;

public class LocalPathPluginSource
        implements PluginSource
{
    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig) throws PluginSourceNotMatchException
    {
        // TODO
        throw new PluginSourceNotMatchException();
    }
}
