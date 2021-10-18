package org.embulk.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.embulk.EmbulkSystemProperties;
import org.embulk.deps.EmbulkSelfContainedJarFiles;
import org.embulk.plugin.DefaultPluginType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches self-contained plugin's loaded classes such as embulk-ruby's Embulk::PluginRegistry.
 *
 * <p>See {@code /embulk-ruby/lib/embulk/plugin_registry.rb}.
 */
final class SelfContainedPluginRegistry {
    private SelfContainedPluginRegistry(
            final Class<?> pluginInterface,
            final String category,
            final EmbulkSystemProperties embulkSystemProperties,
            final PluginClassLoaderFactory pluginClassLoaderFactory) {
        this.cacheMap = new HashMap<DefaultPluginType, Class<?>>();

        this.pluginInterface = pluginInterface;
        this.category = category;

        this.embulkSystemProperties = embulkSystemProperties;
        this.pluginClassLoaderFactory = pluginClassLoaderFactory;
    }

    static Map<Class<?>, SelfContainedPluginRegistry> generateRegistries(
            final EmbulkSystemProperties embulkSystemProperties,
            final PluginClassLoaderFactory pluginClassLoaderFactory) {
        final HashMap<Class<?>, SelfContainedPluginRegistry> registries = new HashMap<>();
        for (final Map.Entry<Class<?>, String> entry : CATEGORIES.entrySet()) {
            registries.put(
                    entry.getKey(),
                    new SelfContainedPluginRegistry(entry.getKey(), entry.getValue(), embulkSystemProperties, pluginClassLoaderFactory));
        }
        return Collections.unmodifiableMap(registries);
    }

    Class<?> lookup(final DefaultPluginType pluginType) throws PluginSourceNotMatchException  {
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

    private Class<?> search(final DefaultPluginType pluginType) throws PluginSourceNotMatchException {
        final String selfContainedPluginName = "embulk-" + this.category + "-" + pluginType.getName();
        if (!EmbulkSelfContainedJarFiles.has(selfContainedPluginName)) {
            throw new PluginSourceNotMatchException();
        }

        final Class<?> pluginMainClass;
        try (final JarPluginLoader loader = JarPluginLoader.loadSelfContained(selfContainedPluginName, this.pluginClassLoaderFactory)) {
            final Class<?> loadedClass = loader.getPluginMainClass();
            logger.info("Loaded plugin {}", selfContainedPluginName);
            return loadedClass;
        } catch (final InvalidJarPluginException ex) {
            throw new PluginSourceNotMatchException(ex);
        }
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

    private static final Logger logger = LoggerFactory.getLogger(SelfContainedPluginRegistry.class);

    private final HashMap<DefaultPluginType, Class<?>> cacheMap;

    private final Class<?> pluginInterface;  // InputPlugin, OutputPlugin, FilterPlugin, ...
    private final String category;

    private final EmbulkSystemProperties embulkSystemProperties;
    private final PluginClassLoaderFactory pluginClassLoaderFactory;
}
