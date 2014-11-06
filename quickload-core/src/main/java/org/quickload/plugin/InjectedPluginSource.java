package org.quickload.plugin;

import java.io.IOException;
import javax.validation.constraints.NotNull;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.quickload.config.ModelManager;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.ConfigException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    public static class PluginSourceTask
    {
        private final String injected;

        @JsonCreator
        public PluginSourceTask(
                @JsonProperty("injected") String injected)
        {
            this.injected = injected;
        }

        @JsonProperty("injected")
        public String getInjected()
        {
            return injected;
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

    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig) throws PluginSourceNotMatchException
    {
        if (!typeConfig.isObject()) {
            throw new PluginSourceNotMatchException();
        }

        PluginSourceTask task;
        try {
            task = modelManager.readJsonObject((ObjectNode) typeConfig, PluginSourceTask.class);
        } catch (RuntimeException ex) {
            // TODO throw PluginSourceNotMatchException if injected field does not exist
            throw ex;
        }

        return injector.getInstance(Key.get(iface, Names.named(task.getInjected())));
    }
}
