package org.embulk.plugin;

public interface PluginSource
{
    <T> T newPlugin(Class<T> iface, PluginType type) throws PluginSourceNotMatchException;
}
