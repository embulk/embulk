package org.embulk.plugin.maven;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.embulk.EmbulkSystemProperties;
import org.embulk.deps.maven.MavenArtifactFinder;
import org.embulk.deps.maven.MavenPluginPaths;
import org.embulk.plugin.DefaultPluginType;
import org.embulk.plugin.MavenPluginType;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginManager;
import org.embulk.plugin.PluginSource;
import org.embulk.plugin.PluginSourceNotMatchException;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.jar.InvalidJarPluginException;
import org.embulk.plugin.jar.JarPluginLoader;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.FileOutputRunner;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenPluginSource implements PluginSource {
    public MavenPluginSource(
            final EmbulkSystemProperties embulkSystemProperties,
            final PluginClassLoaderFactory pluginClassLoaderFactory) {
        this.embulkSystemProperties = embulkSystemProperties;
        this.pluginClassLoaderFactory = pluginClassLoaderFactory;
    }

    @Override
    public <T> T newPlugin(Class<T> pluginInterface, PluginType pluginType)
            throws PluginSourceNotMatchException {
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

        final MavenArtifactFinder mavenArtifactFinder;
        try {
            mavenArtifactFinder = MavenArtifactFinder.create(getLocalMavenRepository());
        } catch (final FileNotFoundException ex) {
            throw new PluginSourceNotMatchException(ex);
        }

        final MavenPluginPaths pluginPaths;
        try {
            pluginPaths = mavenArtifactFinder.findMavenPluginJarsWithDirectDependencies(
                    mavenPluginType.getGroup(),
                    "embulk-" + category + "-" + mavenPluginType.getName(),
                    mavenPluginType.getClassifier(),
                    mavenPluginType.getVersion());
        } catch (final FileNotFoundException ex) {
            throw new PluginSourceNotMatchException(ex);
        }

        final Class<?> pluginMainClass;
        try (JarPluginLoader loader = JarPluginLoader.load(
                 pluginPaths.getPluginJarPath(),
                 pluginPaths.getPluginDependencyJarPaths(),
                 this.pluginClassLoaderFactory)) {
            pluginMainClass = loader.getPluginMainClass();
        } catch (InvalidJarPluginException ex) {
            throw new PluginSourceNotMatchException(ex);
        }

        final Object pluginMainObject;
        try {
            // Unlike JRubyPluginSource,
            // MavenPluginSource does not have "registration" before creating an instance of the plugin class.
            // FileInputPlugin and FileOutputPlugin are wrapped with FileInputRunner and FileOutputRunner here.
            if (FileInputPlugin.class.isAssignableFrom(pluginMainClass)) {
                final FileInputPlugin fileInputPluginMainObject;
                try {
                    fileInputPluginMainObject = (FileInputPlugin) PluginManager.newPluginInstance(
                            pluginMainClass, this.embulkSystemProperties);
                } catch (ClassCastException ex) {
                    throw new PluginSourceNotMatchException(
                            "[FATAL/INTERNAL] Plugin class \"" + pluginMainClass.getName() + "\" is not file-input.",
                            ex);
                }
                pluginMainObject = new FileInputRunner(fileInputPluginMainObject);
            } else if (FileOutputPlugin.class.isAssignableFrom(pluginMainClass)) {
                final FileOutputPlugin fileOutputPluginMainObject;
                try {
                    fileOutputPluginMainObject = (FileOutputPlugin) PluginManager.newPluginInstance(
                            pluginMainClass, this.embulkSystemProperties);
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
                pluginMainObject = PluginManager.newPluginInstance(pluginMainClass, this.embulkSystemProperties);
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

        logger.info("Loaded plugin {} ({})",
                    "embulk-" + category + "-" + mavenPluginType.getName(),
                    mavenPluginType.getFullName());

        try {
            return pluginInterface.cast(pluginMainObject);
        } catch (ClassCastException ex) {
            throw new PluginSourceNotMatchException(
                    "[FATAL/INTERNAL] Plugin class \"" + pluginMainClass.getName() + "\" is not " + category + " actually.",
                    ex);
        }
    }

    private Path getLocalMavenRepository() throws PluginSourceNotMatchException {
        // It expects the Embulk system property "m2_repo" is set from org.embulk.cli.EmbulkSystemPropertiesBuilder.
        final String m2Repo = this.embulkSystemProperties.getProperty("m2_repo", null);
        if (m2Repo == null) {
            throw new PluginSourceNotMatchException("Embulk system property \"m2_repo\" is not set properly.");
        }

        return Paths.get(m2Repo);
    }

    private static final Logger logger = LoggerFactory.getLogger(MavenPluginSource.class);

    private final EmbulkSystemProperties embulkSystemProperties;
    private final PluginClassLoaderFactory pluginClassLoaderFactory;
}
