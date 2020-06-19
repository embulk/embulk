package org.embulk.plugin;

import java.net.URL;
import java.util.Collection;

class PluginClassLoaderFactoryImpl implements PluginClassLoaderFactory {
    PluginClassLoaderFactoryImpl(
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources) {
        this.parentFirstPackages = parentFirstPackages;
        this.parentFirstResources = parentFirstResources;
    }

    @Override
    public PluginClassLoader create(final Collection<URL> urls, final ClassLoader parentClassLoader) {
        return PluginClassLoader.createForFlatJars(
                parentClassLoader,
                urls,
                parentFirstPackages,
                parentFirstResources);
    }

    @Override
    public PluginClassLoader createForNestedJar(
            final ClassLoader parentClassLoader,
            final URL oneNestedJarUrl) {
        return PluginClassLoader.createForNestedJar(
                parentClassLoader,
                oneNestedJarUrl,
                null,
                parentFirstPackages,
                parentFirstResources);
    }

    @Override
    public PluginClassLoader createForNestedJarWithDependencies(
            final ClassLoader parentClassLoader,
            final URL oneNestedJarUrl,
            final Collection<URL> dependencyJarUrls) {
        return PluginClassLoader.createForNestedJar(
                parentClassLoader,
                oneNestedJarUrl,
                dependencyJarUrls,
                parentFirstPackages,
                parentFirstResources);
    }

    private final Collection<String> parentFirstPackages;
    private final Collection<String> parentFirstResources;
}
