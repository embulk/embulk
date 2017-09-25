package org.embulk.plugin;

import java.util.Collection;
import java.net.URL;

public interface PluginClassLoaderFactory
{
    PluginClassLoader create(Collection<URL> flatJarUrls, ClassLoader parentClassLoader);
    PluginClassLoader createForNestedJar(
        final ClassLoader parentClassLoader,
        final URL oneNestedJarUrl);
    PluginClassLoader createForNestedJar(
        final ClassLoader parentClassLoader,
        final URL oneNestedJarUrl,
        final Collection<String> embeddedJarPathsInNestedJar);
}
