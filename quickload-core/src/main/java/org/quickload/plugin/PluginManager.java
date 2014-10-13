package org.quickload.plugin;

import java.util.Set;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.ConfigException;
import org.quickload.spi.Task;

public class PluginManager
{
    private final List<PluginSource> sources;
    private final Injector injector;

    // Set<PluginSource> is injected BuiltinPluginSourceModule or extensions
    // using Multibinder<PluginSource>.
    @Inject
    public PluginManager(Set<PluginSource> pluginSources, Injector injector)
    {
        this.sources = ImmutableList.copyOf(pluginSources);
        this.injector = injector;
    }

    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig)
    {
        for (PluginSource source : sources) {
            try {
                return source.newPlugin(iface, typeConfig);
            } catch (PluginSourceNotMatchException e) {
            }
        }

        throw new ConfigException("Plugin not found");  // TODO exception message should include typeConfig in original format
    }
}
