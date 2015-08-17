package org.embulk.plugin;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.embulk.config.ConfigException;
import org.embulk.spi.InputPlugin;
import org.embulk.plugin.compat.PluginWrappers;

public class PluginManager
{
    private final List<PluginSource> sources;
    private final Injector injector;

    // Set<PluginSource> is injected by BuiltinPluginSourceModule or extensions
    // using Multibinder<PluginSource>.
    @Inject
    public PluginManager(Set<PluginSource> pluginSources, Injector injector)
    {
        this.sources = ImmutableList.copyOf(pluginSources);
        this.injector = injector;
    }

    @SuppressWarnings("unchecked")
    public <T> T newPlugin(Class<T> iface, PluginType type)
    {
        T t = newPluginWithoutWrapper(iface, type);
        if (t instanceof InputPlugin) {
            return (T) PluginWrappers.inputPlugin((InputPlugin) t);
        }
        return t;
    }

    private <T> T newPluginWithoutWrapper(Class<T> iface, PluginType type)
    {
        if (sources.isEmpty()) {
            throw new ConfigException("No PluginSource is installed");
        }

        if (type == null) {
            throw new ConfigException(String.format("%s type is not set (if you intend to use NullOutputPlugin, you should enclose null in quotes such as {type: \"null\"}.", iface.getSimpleName()));
        }

        List<PluginSourceNotMatchException> exceptions = new ArrayList<>();
        for (PluginSource source : sources) {
            try {
                return source.newPlugin(iface, type);
            }
            catch (PluginSourceNotMatchException e) {
                exceptions.add(e);
            }
        }

        throw buildPluginNotFoundException(iface, type, exceptions);
    }

    private static ConfigException buildPluginNotFoundException(Class<?> iface, PluginType type,
            List<PluginSourceNotMatchException> exceptions)
    {
        StringBuilder message = new StringBuilder();
        message.append(String.format("%s '%s' is not found.", iface.getSimpleName(), type.getName()));
        for (PluginSourceNotMatchException exception : exceptions) {
            Throwable cause = (exception.getCause() == null ? exception : exception.getCause());
            if (cause.getMessage() != null) {
                message.append(String.format("%n"));
                message.append(cause.getMessage());
            }
        }
        ConfigException e = new ConfigException(message.toString());
        for (PluginSourceNotMatchException exception : exceptions) {
            e.addSuppressed(exception);
        }
        return e;
    }
}
