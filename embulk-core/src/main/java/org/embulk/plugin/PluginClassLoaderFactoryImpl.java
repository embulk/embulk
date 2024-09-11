package org.embulk.plugin;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

public class PluginClassLoaderFactoryImpl implements PluginClassLoaderFactory {
    private PluginClassLoaderFactoryImpl() {
        this.createdPluginClassLoaders = new ArrayList<>();
    }

    public static PluginClassLoaderFactoryImpl of() {
        return new PluginClassLoaderFactoryImpl();
    }

    @Override
    public PluginClassLoader create(final Collection<URL> urls, final ClassLoader parentClassLoader) {
        final PluginClassLoader created = PluginClassLoader.create(
                parentClassLoader,
                urls);
        this.createdPluginClassLoaders.add(created);
        return created;
    }

    @Override
    public PluginClassLoader forSelfContainedPlugin(final String selfContainedPluginName, final ClassLoader parentClassLoader) {
        final PluginClassLoader created = PluginClassLoader.forSelfContainedPlugin(
                parentClassLoader,
                selfContainedPluginName);
        this.createdPluginClassLoaders.add(created);
        return created;
    }

    @Override
    public void clear() {
        // "close()" is intentionally not called for them considering: https://bugs.openjdk.java.net/browse/JDK-8246714
        /*
        for (final PluginClassLoader pluginClassLoader : this.createdPluginClassLoaders) {
            pluginClassLoader.close();
        }
        */
        this.createdPluginClassLoaders.clear();
    }

    // Created PluginClassLoaders are maintained in the list so that they are not garbage-collected accidentally.
    private final ArrayList<PluginClassLoader> createdPluginClassLoaders;
}
