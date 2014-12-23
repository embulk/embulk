package org.embulk.plugin;

import java.util.Set;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.embulk.config.ConfigException;

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

    public <T> T newPlugin(Class<T> iface, PluginType type)
    {
        for (PluginSource source : sources) {
            try {
                return source.newPlugin(iface, type);
            } catch (PluginSourceNotMatchException e) {
            }
        }
        throw new ConfigException("Plugin not found");  // TODO exception message should include type in original format
    }
}
