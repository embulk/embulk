package org.quickload.plugin;

import java.io.IOException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.quickload.config.ModelManager;
import org.quickload.config.ConfigException;

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
    public static class Task <T>
    {
        private final String name;

        @JsonCreator
        public Task(@JsonProperty("name") String name)
        {
            this.name = name;
        }

        @JsonProperty("name")
        public String getName()
        {
            return name;
        }
    }

    private final ModelManager modelManager;
    private final Injector injector;

    @Inject
    public InjectedPluginSource(
            ModelManager modelManager,
            Injector injector)
    {
        this.modelManager = modelManager;
        this.injector = injector;
    }

    public <T> T newPlugin(Class<T> iface, String configExpression) throws PluginSourceNotMatchException
    {
        // TODO cache

        Task<T> task;
        try {
            task = (Task<T>) modelManager.getObjectMapper().readValue(configExpression, Task.class);
        } catch (IOException ex) {
            // TODO throw PluginSourceNotMatchException if injected field does not exist
            throw new ConfigException(ex);
        }

        return injector.getInstance(Key.get(iface, Names.named(task.getName())));
    }
}
