package org.embulk.jar;

import java.io.InputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.embulk.plugin.PluginClassLoader;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginSource;
import org.embulk.plugin.PluginSourceNotMatchException;
import org.embulk.plugin.PluginType;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;

public class JarPluginSource
        implements PluginSource
{
    @Inject
    public JarPluginSource(Injector injector)
    {
        this.injector = injector;
    }

    @Override
    public <T> T newPlugin(Class<T> pluginInterface, PluginType pluginType)
            throws PluginSourceNotMatchException
    {
        final String category;
        if (InputPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "input";
        } else if (OutputPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "output";
        } else if (ParserPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "parser";
        } else if (FormatterPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "formatter";
        } else if (DecoderPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "decoder";
        } else if (EncoderPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "encoder";
        } else if (FilterPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "filter";
        } else if (GuessPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "guess";
        } else if (ExecutorPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "executor";
        } else {
            // unsupported plugin category
            throw new PluginSourceNotMatchException("Plugin interface " + pluginInterface + " is not supported.");
        }

        if (!pluginType.getName().startsWith("jar")) {
            throw new PluginSourceNotMatchException("\"type\" does not start with \"jar:\".");
        }
        final Path jarPath = Paths.get(pluginType.getName().substring(4));

        final URL jarUrl;
        final URL urlPluginProperties;
        try {
            jarUrl = jarPath.toUri().toURL();
            urlPluginProperties = new URL("jar:" + jarPath.toUri().toURL().toString() + "!/plugin.properties");
        }
        catch (MalformedURLException ex) {
            throw new PluginSourceNotMatchException("Invalid plugin path: " + jarPath.toString(), ex);
        }

        final Properties pluginProperties = new Properties();
        try (final InputStream inputFromProperties =
             ((JarURLConnection) urlPluginProperties.openConnection()).getInputStream()) {
            pluginProperties.load(inputFromProperties);
        }
        catch (IOException ex) {
            throw new PluginSourceNotMatchException("Invalid plugin: \"plugin.properties\" not accessible.", ex);
        }

        final String pluginMainClassName = pluginProperties.getProperty("pluginMainClass");
        if (pluginMainClassName == null) {
            throw new PluginSourceNotMatchException("Invalid plugin: \"pluginMainClass\" not in plugin.properties.");
        }

        final PluginClassLoaderFactory pluginClassLoaderFactory =
            this.injector.getInstance(PluginClassLoaderFactory.class);
        final PluginClassLoader pluginClassLoader =
            pluginClassLoaderFactory.create(ImmutableList.of(jarUrl), JarPluginSource.class.getClassLoader());
        final Class pluginMainClass;
        try {
            pluginMainClass = pluginClassLoader.loadClass(pluginMainClassName);
        }
        catch (ClassNotFoundException ex) {
            throw new PluginSourceNotMatchException("Invalid plugin: \"" + pluginMainClassName+ "\" not loadable.", ex);
        }

        if (!pluginInterface.isAssignableFrom(pluginMainClass)) {
            throw new PluginSourceNotMatchException(
                "Invalid plugin: \"" + pluginMainClassName + "\" is not " + category + ".");
        }
        // TODO(dmikurube): Inject for each plugin as described in plugin.properties.

        final Object pluginMainObject;
        try {
            pluginMainObject = pluginMainClass.newInstance();
        }
        catch (InstantiationException ex) {
            throw new PluginSourceNotMatchException(
                "Invalid plugin: \"" + pluginMainClassName + "\" not instantiatable.", ex);
        }
        catch (IllegalAccessException ex) {
            throw new PluginSourceNotMatchException(
                "Invalid plugin: \"" + pluginMainClassName + "\" illegal access.", ex);
        }

        try {
            return pluginInterface.cast(pluginMainObject);
        }
        catch (ClassCastException ex) {
            throw new PluginSourceNotMatchException(
                "FATAL: Invalid plugin: \"" + pluginMainClassName + "\" is not " + category + ".");
        }
    }

    private final Injector injector;
}
