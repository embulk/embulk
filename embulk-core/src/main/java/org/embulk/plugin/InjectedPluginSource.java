package org.embulk.plugin;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.FileOutputRunner;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;

/**
 * InjectedPluginSource loads plugins bound by Guice.
 * This plugin source is intended to be used in test cases.
 * Plugins need to be bound to Binder following:
 *
 * // Module
 * public void configure(Binder binder)
 * {
 *     InjectedPluginSource.registerPluginTo(InputPluginclass, "my", MyInputPlugin.class);
 * }
 *
 */
public class InjectedPluginSource implements PluginSource {
    private final Injector injector;

    @Inject
    public InjectedPluginSource(Injector injector) {
        this.injector = injector;
    }

    private interface PluginFactory<T> {
        T newPlugin(Injector injector);
    }

    public <T> T newPlugin(Class<T> iface, PluginType type) throws PluginSourceNotMatchException {
        if (type.getSourceType() != PluginSource.Type.DEFAULT) {
            throw new PluginSourceNotMatchException();
        }

        final String name = type.getName();
        try {
            @SuppressWarnings("unchecked")
            PluginFactory<T> factory = (PluginFactory<T>) injector.getInstance(
                    Key.get(PluginFactory.class, pluginFactoryName(iface, name)));
            return factory.newPlugin(injector);
        } catch (com.google.inject.ConfigurationException ex) {
            throw new PluginSourceNotMatchException();
        }
    }

    public static <T> void registerPluginTo(Binder binder, Class<T> iface, String name, final Class<?> impl) {
        PluginFactory<T> factory;
        if (FileInputPlugin.class.isAssignableFrom(impl)) {
            Preconditions.checkArgument(InputPlugin.class.equals(iface));
            factory = injector -> {
                @SuppressWarnings("unchecked")
                T result = (T) new FileInputRunner((FileInputPlugin) injector.getInstance(impl));
                return result;
            };
        } else if (FileOutputPlugin.class.isAssignableFrom(impl)) {
            Preconditions.checkArgument(OutputPlugin.class.equals(iface));
            factory = injector -> {
                @SuppressWarnings("unchecked")
                T result = (T) new FileOutputRunner((FileOutputPlugin) injector.getInstance(impl));
                return result;
            };
        } else {
            Preconditions.checkArgument(iface.isAssignableFrom(impl));
            factory = injector -> {
                @SuppressWarnings("unchecked")
                T result = (T) injector.getInstance(impl);
                return result;
            };
        }
        binder.bind(PluginFactory.class).annotatedWith(pluginFactoryName(iface, name)).toInstance(factory);
    }

    private static Named pluginFactoryName(Class<?> iface, String name) {
        return Names.named(iface.getName() + "." + name);
    }
}
