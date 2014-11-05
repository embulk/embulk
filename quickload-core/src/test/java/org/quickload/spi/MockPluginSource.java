package org.quickload.plugin;

import com.fasterxml.jackson.databind.JsonNode;

public class MockPluginSource
{
    private final Class<?> expectedIface;
    private final Object plugin;
    private JsonNode typeConfig;

    public <T> MockPluginSource(Class<T> expectedIface, T plugin)
    {
        this.expectedIface = expectedIface;
        this.plugin = plugin;
    }

    public JsonNode getTypeConfig()
    {
        return typeConfig;
    }

    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig) throws PluginSourceNotMatchException
    {
        if (expectedIface.equals(iface)) {
            this.typeConfig = typeConfig;
            return (T) plugin;
        } else {
            throw new PluginSourceNotMatchException();
        }
    }
}
