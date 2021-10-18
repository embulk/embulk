package org.embulk.plugin.maven;

import com.google.inject.Injector;
import java.util.Map;
import org.embulk.EmbulkSystemProperties;
import org.embulk.plugin.DefaultPluginType;
import org.embulk.plugin.MavenPluginType;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginSource;
import org.embulk.plugin.PluginSourceNotMatchException;
import org.embulk.plugin.PluginType;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.FileOutputRunner;

public class MavenPluginSource implements PluginSource {
    public MavenPluginSource(
            final Injector injector,
            final EmbulkSystemProperties embulkSystemProperties,
            final PluginClassLoaderFactory pluginClassLoaderFactory) {
        this.injector = injector;
        this.embulkSystemProperties = embulkSystemProperties;
        this.registries = MavenPluginRegistry.generateRegistries(embulkSystemProperties, pluginClassLoaderFactory);
    }

    @Override
    public <T> T newPlugin(Class<T> pluginInterface, PluginType pluginType)
            throws PluginSourceNotMatchException {
        final MavenPluginRegistry registry = this.registries.get(pluginInterface);
        if (registry == null) {
            // unsupported plugin category
            throw new PluginSourceNotMatchException("Plugin interface " + pluginInterface + " is not supported.");
        }
        final String category = registry.getCategory();

        final MavenPluginType mavenPluginType;
        switch (pluginType.getSourceType()) {
            case DEFAULT:
                mavenPluginType = MavenPluginType.createFromDefaultPluginType(
                        category, (DefaultPluginType) pluginType, this.embulkSystemProperties);
                break;

            case MAVEN:
                mavenPluginType = (MavenPluginType) pluginType;
                break;

            default:
                throw new PluginSourceNotMatchException();
        }

        final Class<?> pluginMainClass = registry.lookup(mavenPluginType);

        final Object pluginMainObject;
        try {
            // Unlike JRubyPluginSource,
            // MavenPluginSource does not have "registration" before creating an instance of the plugin class.
            // FileInputPlugin and FileOutputPlugin are wrapped with FileInputRunner and FileOutputRunner here.
            if (FileInputPlugin.class.isAssignableFrom(pluginMainClass)) {
                final FileInputPlugin fileInputPluginMainObject;
                try {
                    fileInputPluginMainObject = (FileInputPlugin) this.injector.getInstance(pluginMainClass);
                } catch (ClassCastException ex) {
                    throw new PluginSourceNotMatchException(
                            "[FATAL/INTERNAL] Plugin class \"" + pluginMainClass.getName() + "\" is not file-input.",
                            ex);
                }
                pluginMainObject = new FileInputRunner(fileInputPluginMainObject);
            } else if (FileOutputPlugin.class.isAssignableFrom(pluginMainClass)) {
                final FileOutputPlugin fileOutputPluginMainObject;
                try {
                    fileOutputPluginMainObject = (FileOutputPlugin) this.injector.getInstance(pluginMainClass);
                } catch (ClassCastException ex) {
                    throw new PluginSourceNotMatchException(
                            "[FATAL/INTERNAL] Plugin class \"" + pluginMainClass.getName() + "\" is not file-output.",
                            ex);
                }
                pluginMainObject = new FileOutputRunner(fileOutputPluginMainObject);
            } else {
                if (!pluginInterface.isAssignableFrom(pluginMainClass)) {
                    throw new PluginSourceNotMatchException(
                            "Plugin class \"" + pluginMainClass.getName() + "\" is not a valid " + category + " plugin.");
                }
                pluginMainObject = this.injector.getInstance(pluginMainClass);
            }
        } catch (ExceptionInInitializerError ex) {
            throw new PluginSourceNotMatchException(
                    "Plugin class \"" + pluginMainClass.getName()
                            + "\" is not instantiatable due to exception in initialization.",
                    ex);
        } catch (SecurityException ex) {
            throw new PluginSourceNotMatchException(
                    "Plugin class \"" + pluginMainClass.getName()
                            + "\" is not instantiatable due to security manager.",
                    ex);
        }

        try {
            return pluginInterface.cast(pluginMainObject);
        } catch (ClassCastException ex) {
            throw new PluginSourceNotMatchException(
                    "[FATAL/INTERNAL] Plugin class \"" + pluginMainClass.getName() + "\" is not " + category + " actually.",
                    ex);
        }
    }

    private final Injector injector;
    private final EmbulkSystemProperties embulkSystemProperties;
    private final Map<Class<?>, MavenPluginRegistry> registries;
}
