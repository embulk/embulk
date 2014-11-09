package org.quickload.plugin;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * InjectedPluginSource loads plugins bound by Guice.
 * This plugin source is intended to be used in test cases.
 * Plugins need to be bound to Binder with Name annotation as following:
 *
 * // Module
 * public void configure(Binder binder)
 * {
 *     bind(InputPlugin.class)
 *              .annotatedWith(Names.named("my"))
 *              .to(MyInputPlugin.class);
 * }
 *
 */
public class InjectedPluginSource
        implements PluginSource
{
    private final Injector injector;

    @Inject
    public InjectedPluginSource(
            Injector injector)
    {
        this.injector = injector;
    }

    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig) throws PluginSourceNotMatchException
    {
        if (!typeConfig.isTextual()) {
            throw new PluginSourceNotMatchException();
        }
        String name = typeConfig.asText();
        try {
            return injector.getInstance(Key.get(iface, Names.named(name)));
        } catch (com.google.inject.ConfigurationException ex) {
            throw new PluginSourceNotMatchException();
        }
    }
}
