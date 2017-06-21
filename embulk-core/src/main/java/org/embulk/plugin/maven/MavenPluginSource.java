package org.embulk.plugin.maven;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.embulk.plugin.MavenPluginType;
import org.embulk.plugin.PluginClassLoader;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginSource;
import org.embulk.plugin.PluginSourceNotMatchException;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.jar.JarPluginLoader;
import org.embulk.plugin.jar.InvalidJarPluginException;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;

public class MavenPluginSource
        implements PluginSource
{
    @Inject
    public MavenPluginSource(Injector injector)
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

        if (pluginType.getSourceType() != PluginSource.Type.MAVEN) {
            throw new PluginSourceNotMatchException();
        }
        final MavenPluginType mavenPluginType = (MavenPluginType) pluginType;

        final PluginClassLoaderFactory pluginClassLoaderFactory =
            this.injector.getInstance(PluginClassLoaderFactory.class);

        final MavenArtifactFinder mavenArtifactFinder;
        try {
            mavenArtifactFinder = MavenArtifactFinder.create(getLocalMavenRepository());
        }
        catch (MavenRepositoryNotFoundException ex) {
            throw new PluginSourceNotMatchException(ex);
        }

        final Path jarPath;
        try {
            jarPath = mavenArtifactFinder.findMavenArtifactJar(
                mavenPluginType.getGroup(),
                "embulk-" + category + "-" + mavenPluginType.getName(),
                mavenPluginType.getVersion());
        }
        catch (MavenArtifactNotFoundException ex) {
            throw new PluginSourceNotMatchException(ex);
        }

        final Class pluginMainClass;
        try (JarPluginLoader loader = JarPluginLoader.load(jarPath, pluginClassLoaderFactory)) {
            pluginMainClass = loader.getPluginMainClass();
        }
        catch (InvalidJarPluginException ex) {
            throw new PluginSourceNotMatchException(ex);
        }

        if (!pluginInterface.isAssignableFrom(pluginMainClass)) {
            throw new PluginSourceNotMatchException(
                "Invalid plugin: \"" + pluginMainClass.getName() + "\" is not " + category + ".");
        }
        // TODO(dmikurube): Inject for each plugin as described in plugin.properties.

        final Object pluginMainObject;
        try {
            pluginMainObject = pluginMainClass.newInstance();
        }
        catch (InstantiationException ex) {
            throw new PluginSourceNotMatchException(
                "Invalid plugin: \"" + pluginMainClass.getName() + "\" not instantiatable.", ex);
        }
        catch (IllegalAccessException ex) {
            throw new PluginSourceNotMatchException(
                "Invalid plugin: \"" + pluginMainClass.getName() + "\" illegal access.", ex);
        }

        try {
            return pluginInterface.cast(pluginMainObject);
        }
        catch (ClassCastException ex) {
            throw new PluginSourceNotMatchException(
                "FATAL: Invalid plugin: \"" + pluginMainClass.getName() + "\" is not " + category + ".");
        }
    }

    private Path getLocalMavenRepository()
            throws PluginSourceNotMatchException
    {
        return getEmbulkHome().resolve("m2").resolve("repository");
    }

    private Path getEmbulkHome()
            throws PluginSourceNotMatchException
    {
        final String propertyHome = System.getProperty("user.home");
        if (propertyHome == null) {
            throw new PluginSourceNotMatchException();
        }

        return Paths.get(propertyHome, ".embulk");
    }

    private final Injector injector;
}
