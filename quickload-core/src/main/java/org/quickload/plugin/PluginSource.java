package org.quickload.plugin;

public interface PluginSource
{
    public <T> T newPlugin(Class<T> iface, String configExpression) throws PluginSourceNotMatchException;
}
