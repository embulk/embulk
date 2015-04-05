package org.embulk.plugin;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
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
        if (sources.isEmpty()) {
            throw new ConfigException("No PluginSource is installed");
        }

        if (type == null) {
            throw new ConfigException(String.format("%s type is not set (if you intend to use NullOutputPlugin, you should enclose null in quotes such as {type: \"null\"}.", iface.getSimpleName()));
        }

        List<Throwable> causes = new ArrayList<Throwable>();
        for (PluginSource source : sources) {
            try {
                return source.newPlugin(iface, type);
            } catch (PluginSourceNotMatchException e) {
                if (e.getCause() != null) {
                    causes.add(e.getCause());
                } else {
                    causes.add(e);
                }
            }
        }

        StringBuilder message = new StringBuilder();
        message.append(String.format("%s '%s' is not found.", iface.getSimpleName(), type.getName()));
        for (Throwable cause : causes) {
            if (cause.getMessage() != null) {
                message.append(String.format("%n"));
                message.append(cause.getMessage());
            }
        }
        ConfigException e = new ConfigException(message.toString());
        for (Throwable cause : causes) {
            e.addSuppressed(cause);
        }
        throw e;
    }
}
