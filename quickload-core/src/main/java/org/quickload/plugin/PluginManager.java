package org.quickload.plugin;

import java.util.Set;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.quickload.spi.InputPlugin;
import org.quickload.spi.OutputPlugin;
import org.quickload.spi.ParserPlugin;
import org.quickload.spi.FormatterPlugin;
import org.quickload.spi.ParserGuessPlugin;
import org.quickload.spi.LineGuessPlugin;
import org.quickload.config.ConfigException;

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

    public <T> T newPlugin(Class<T> iface, String configExpression)
    {
        for (PluginSource source : sources) {
            try {
                return source.newPlugin(iface, configExpression);
            } catch (PluginSourceNotMatchException e) {
            }
        }

        throw new ConfigException("Plugin not found");  // TODO exception message should include configExpression in original format
    }
}
