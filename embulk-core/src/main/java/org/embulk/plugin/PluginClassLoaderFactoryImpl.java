package org.embulk.plugin;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

public class PluginClassLoaderFactoryImpl implements PluginClassLoaderFactory {
    private PluginClassLoaderFactoryImpl(
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources) {
        this.parentFirstPackages = parentFirstPackages;
        this.parentFirstResources = parentFirstResources;
        this.createdPluginClassLoaders = new ArrayList<>();
    }

    public static PluginClassLoaderFactoryImpl of(
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources) {
        return new PluginClassLoaderFactoryImpl(parentFirstPackages, parentFirstResources);
    }

    @Override
    public PluginClassLoader create(final Collection<URL> urls, final ClassLoader parentClassLoader) {
        final PluginClassLoader created = PluginClassLoader.createForFlatJars(
                parentClassLoader,
                urls,
                parentFirstPackages,
                parentFirstResources);
        this.createdPluginClassLoaders.add(created);
        return created;
    }

    @Override
    public PluginClassLoader createForNestedJar(
            final ClassLoader parentClassLoader,
            final URL oneNestedJarUrl) {
        final PluginClassLoader created = PluginClassLoader.createForNestedJar(
                parentClassLoader,
                oneNestedJarUrl,
                null,
                parentFirstPackages,
                parentFirstResources);
        this.createdPluginClassLoaders.add(created);
        return created;
    }

    @Override
    public PluginClassLoader createForNestedJarWithDependencies(
            final ClassLoader parentClassLoader,
            final URL oneNestedJarUrl,
            final Collection<URL> dependencyJarUrls) {
        final PluginClassLoader created = PluginClassLoader.createForNestedJar(
                parentClassLoader,
                oneNestedJarUrl,
                dependencyJarUrls,
                parentFirstPackages,
                parentFirstResources);
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

    private final Collection<String> parentFirstPackages;
    private final Collection<String> parentFirstResources;

    // Created PluginClassLoaders are maintained in the list so that they are not garbage-collected accidentally.
    private final ArrayList<PluginClassLoader> createdPluginClassLoaders;
}
