package org.embulk.plugin;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.embulk.EmbulkSystemProperties;

public class PluginClassLoaderFactoryImpl implements PluginClassLoaderFactory {
    private PluginClassLoaderFactoryImpl(
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources,
            final boolean hold) {
        this.parentFirstPackages = parentFirstPackages;
        this.parentFirstResources = parentFirstResources;
        if (hold) {
            this.createdPluginClassLoaders = new HashMap<>();
        } else {
            this.createdPluginClassLoaders = new WeakHashMap<>();
        }
    }

    public static PluginClassLoaderFactoryImpl of(
            final EmbulkSystemProperties embulkSystemProperties,
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources) {
        // Set plugins.classloaders.hold = true to hold created PluginClassLoader instances in memory.
        final boolean hold = embulkSystemProperties.getPropertyAsBoolean("plugins.classloaders.hold", true);

        return new PluginClassLoaderFactoryImpl(parentFirstPackages, parentFirstResources, hold);
    }

    public static PluginClassLoaderFactoryImpl forTesting(
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources) {
        return new PluginClassLoaderFactoryImpl(parentFirstPackages, parentFirstResources, false);
    }

    @Override
    public PluginClassLoader create(final Collection<URL> urls, final ClassLoader parentClassLoader) {
        final PluginClassLoader created = PluginClassLoader.create(
                parentClassLoader,
                urls,
                parentFirstPackages,
                parentFirstResources);
        this.createdPluginClassLoaders.put(created, true);
        return created;
    }

    @Override
    public PluginClassLoader forSelfContainedPlugin(final String selfContainedPluginName, final ClassLoader parentClassLoader) {
        final PluginClassLoader created = PluginClassLoader.forSelfContainedPlugin(
                parentClassLoader,
                selfContainedPluginName,
                this.parentFirstPackages,
                this.parentFirstResources);
        this.createdPluginClassLoaders.put(created, true);
        return created;
    }

    @Override
    public void clear() {
        // "close()" is intentionally not called for them considering: https://bugs.openjdk.java.net/browse/JDK-8246714
        /*
        for (final PluginClassLoader pluginClassLoader : this.createdPluginClassLoaders.keySet()) {
            if (pluginClassLoader != null) {
                pluginClassLoader.close();
            }
        }
        */
        this.createdPluginClassLoaders.clear();
    }

    private final Collection<String> parentFirstPackages;
    private final Collection<String> parentFirstResources;

    // Created PluginClassLoaders are maintained in the list so that they are not garbage-collected accidentally.
    private final Map<PluginClassLoader, Boolean> createdPluginClassLoaders;
}
