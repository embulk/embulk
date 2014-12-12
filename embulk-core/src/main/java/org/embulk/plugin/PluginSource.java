package org.embulk.plugin;

import com.fasterxml.jackson.databind.JsonNode;

public interface PluginSource
{
    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig) throws PluginSourceNotMatchException;
}
