package org.embulk.plugin;

public interface PluginSource
{
    public <T> T newPlugin(Class<T> iface, PluginType type) throws PluginSourceNotMatchException;
}
