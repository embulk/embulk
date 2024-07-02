package org.embulk.plugin.maven;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.embulk.EmbulkSystemProperties;
import org.embulk.plugin.DefaultPluginType;
import org.embulk.plugin.MavenPluginType;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginSource;
import org.embulk.plugin.PluginSourceNotMatchException;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.jar.InvalidJarPluginException;
import org.embulk.plugin.jar.JarPluginLoader;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches Maven-based plugin's loaded classes such as embulk-ruby's Embulk::PluginRegistry.
 *
 * <p>See {@code /embulk-ruby/lib/embulk/plugin_registry.rb}.
 */
final class MavenPluginRegistry {
    private MavenPluginRegistry(
            final Class<?> pluginInterface,
            final String category,
            final EmbulkSystemProperties embulkSystemProperties,
            final PluginClassLoaderFactory pluginClassLoaderFactory) {
        this.cacheMap = new HashMap<MavenPluginType, Class<?>>();

        this.pluginInterface = pluginInterface;
        this.category = category;

        this.embulkSystemProperties = embulkSystemProperties;
        this.pluginClassLoaderFactory = pluginClassLoaderFactory;
    }

    static Map<Class<?>, MavenPluginRegistry> generateRegistries(
            final EmbulkSystemProperties embulkSystemProperties,
            final PluginClassLoaderFactory pluginClassLoaderFactory) {
        final HashMap<Class<?>, MavenPluginRegistry> registries = new HashMap<>();
        for (final Map.Entry<Class<?>, String> entry : CATEGORIES.entrySet()) {
            registries.put(
                    entry.getKey(),
                    new MavenPluginRegistry(entry.getKey(), entry.getValue(), embulkSystemProperties, pluginClassLoaderFactory));
        }
        return Collections.unmodifiableMap(registries);
    }

    Class<?> lookup(final PluginType pluginType) throws PluginSourceNotMatchException {
        final MavenPluginPaths pluginPaths = this.findPluginPaths(pluginType);
        final MavenPluginType mavenPluginType = pluginPaths.getPluginType();

        synchronized (this.cacheMap) {  // Synchronize between this.cacheMap.get() and this.cacheMap.put().
            final Class<?> pluginMainClass = this.cacheMap.get(mavenPluginType);
            if (pluginMainClass != null) {
                return pluginMainClass;
            }

            final Class<?> loadedPluginMainClass;
            try (final JarPluginLoader loader = JarPluginLoader.load(
                     pluginPaths.getPluginJarPath(),
                     pluginPaths.getPluginDependencyJarPaths(),
                     this.pluginClassLoaderFactory)) {
                loadedPluginMainClass = loader.getPluginMainClass();
            } catch (final InvalidJarPluginException ex) {
                throw new PluginSourceNotMatchException(ex);
            }

            logger.info("Loaded plugin embulk-{}-{} ({})", 
                        this.category, 
                        mavenPluginType.getName(), 
                        mavenPluginType.getFullName());

            this.cacheMap.put(mavenPluginType, loadedPluginMainClass);
            return loadedPluginMainClass;
        }
    }

    String getCategory() {
        return this.category;
    }

    private MavenPluginPaths findPluginPaths(final PluginType pluginType) throws PluginSourceNotMatchException {
        final MavenArtifactFinder mavenArtifactFinder;
        try {
            mavenArtifactFinder = MavenArtifactFinder.create(this.getLocalMavenRepository());
        } catch (final FileNotFoundException ex) {
            throw new PluginSourceNotMatchException(ex);
        }

        if (pluginType.getSourceType() == PluginSource.Type.DEFAULT) {
            final MavenPluginType nonDefaultMavenPluginType = MavenPluginType.createFromDefaultPluginType(
                    "plugins.", this.category, (DefaultPluginType) pluginType, this.embulkSystemProperties);
            if (nonDefaultMavenPluginType != null) {
                try {
                    return mavenArtifactFinder.findMavenPluginJarsWithDirectDependencies(nonDefaultMavenPluginType, this.category);
                } catch (final FileNotFoundException ex) {
                    logger.warn("Plugin {} specified in \"plugins.{}.{}\" was not found. Falling back to \"plugins.default.{}.{}\".",
                                nonDefaultMavenPluginType.getFullName(),
                                this.category,
                                pluginType.getName(),
                                this.category,
                                pluginType.getName(),
                                ex);
                    // Pass-through.
                }
            }

            final MavenPluginType defaultMavenPluginType = MavenPluginType.createFromDefaultPluginType(
                    "plugins.default.", this.category, (DefaultPluginType) pluginType, this.embulkSystemProperties);
            if (defaultMavenPluginType != null) {
                try {
                    return mavenArtifactFinder.findMavenPluginJarsWithDirectDependencies(defaultMavenPluginType, this.category);
                } catch (final FileNotFoundException ex) {
                    logger.warn("Plugin {} specified in \"plugins.default.{}.{}\" was not found.",
                                defaultMavenPluginType.getFullName(),
                                this.category,
                                pluginType.getName(),
                                ex);
                    throw new PluginSourceNotMatchException(ex);
                }
            }
        } else if (pluginType.getSourceType() == PluginSource.Type.MAVEN) {
            final MavenPluginType mavenPluginType = (MavenPluginType) pluginType;
            try {
                return mavenArtifactFinder.findMavenPluginJarsWithDirectDependencies(mavenPluginType, this.category);
            } catch (final FileNotFoundException ex) {
                throw new PluginSourceNotMatchException(ex);
            }
        }
        throw new PluginSourceNotMatchException();
    }

    private Path getLocalMavenRepository() throws PluginSourceNotMatchException {
        // It expects the Embulk system property "m2_repo" is set from org.embulk.cli.EmbulkSystemPropertiesBuilder.
        final String m2Repo = this.embulkSystemProperties.getProperty("m2_repo", null);
        if (m2Repo == null) {
            throw new PluginSourceNotMatchException("Embulk system property \"m2_repo\" is not set properly.");
        }

        return Paths.get(m2Repo);
    }

    private static final Map<Class<?>, String> CATEGORIES;

    static {
        final HashMap<Class<?>, String> categories = new HashMap<>();
        categories.put(InputPlugin.class, "input");
        categories.put(OutputPlugin.class, "output");
        categories.put(ParserPlugin.class, "parser");
        categories.put(FormatterPlugin.class, "formatter");
        categories.put(DecoderPlugin.class, "decoder");
        categories.put(EncoderPlugin.class, "encoder");
        categories.put(FilterPlugin.class, "filter");
        categories.put(GuessPlugin.class, "guess");
        categories.put(ExecutorPlugin.class, "executor");
        CATEGORIES = Collections.unmodifiableMap(categories);
    }

    private static final Logger logger = LoggerFactory.getLogger(MavenPluginRegistry.class);

    private final HashMap<MavenPluginType, Class<?>> cacheMap;

    private final Class<?> pluginInterface;  // InputPlugin, OutputPlugin, FilterPlugin, ...
    private final String category;

    private final EmbulkSystemProperties embulkSystemProperties;
    private final PluginClassLoaderFactory pluginClassLoaderFactory;
}
