package org.quickload.plugin;

public class LocalPathPluginSource
        implements PluginSource
{
    public <T> T newPlugin(Class<T> iface, String configExpression) throws PluginSourceNotMatchException
    {
        // TODO
        throw new PluginSourceNotMatchException();
    }
}
