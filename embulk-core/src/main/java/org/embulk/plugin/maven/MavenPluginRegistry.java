package org.embulk.plugin.maven;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.embulk.EmbulkSystemProperties;
import org.embulk.deps.maven.MavenArtifactFinder;
import org.embulk.deps.maven.MavenPluginPaths;
import org.embulk.plugin.MavenPluginType;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginSourceNotMatchException;
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

    Class<?> lookup(final MavenPluginType pluginType) throws PluginSourceNotMatchException  {
        synchronized (this.cacheMap) {  // Synchronize between this.cacheMap.get() and this.cacheMap.put().
            final Class<?> pluginMainClass = this.cacheMap.get(pluginType);
            if (pluginMainClass != null) {
                return pluginMainClass;
            }

            final Class<?> loadedPluginMainClass = this.search(pluginType);
            this.cacheMap.put(pluginType, loadedPluginMainClass);
            return loadedPluginMainClass;
        }
    }

    String getCategory() {
        return this.category;
    }

    private Class<?> search(final MavenPluginType pluginType) throws PluginSourceNotMatchException {
        final MavenArtifactFinder mavenArtifactFinder;
        try {
            mavenArtifactFinder = MavenArtifactFinder.create(this.getLocalMavenRepository());
        } catch (final FileNotFoundException ex) {
            throw new PluginSourceNotMatchException(ex);
        }

        final MavenPluginPaths pluginPaths;
        try {
            pluginPaths = mavenArtifactFinder.findMavenPluginJarsWithDirectDependencies(
                    pluginType.getGroup(),
                    "embulk-" + category + "-" + pluginType.getName(),
                    pluginType.getClassifier(),
                    pluginType.getVersion());
        } catch (final FileNotFoundException ex) {
            throw new PluginSourceNotMatchException(ex);
        }

        try (final JarPluginLoader loader = JarPluginLoader.load(
                 pluginPaths.getPluginJarPath(),
                 pluginPaths.getPluginDependencyJarPaths(),
                 this.pluginClassLoaderFactory)) {
            return loader.getPluginMainClass();
        } catch (final InvalidJarPluginException ex) {
            throw new PluginSourceNotMatchException(ex);
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

    private static String lookupCategory(final Class<?> pluginInterface) throws PluginSourceNotMatchException {
        for (final Map.Entry<Class<?>, String> entry : CATEGORIES.entrySet()) {
            if (entry.getKey().isAssignableFrom(pluginInterface)) {
                return entry.getValue();
            }
        }
        // unsupported plugin category
        throw new PluginSourceNotMatchException("Plugin interface " + pluginInterface + " is not supported.");
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

    private final HashMap<MavenPluginType, Class<?>> cacheMap;

    private final Class<?> pluginInterface;  // InputPlugin, OutputPlugin, FilterPlugin, ...
    private final String category;

    private final EmbulkSystemProperties embulkSystemProperties;
    private final PluginClassLoaderFactory pluginClassLoaderFactory;
}
