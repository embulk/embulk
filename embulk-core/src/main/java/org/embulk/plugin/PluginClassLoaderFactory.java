package org.embulk.plugin;

import java.net.URL;
import java.util.Collection;

public interface PluginClassLoaderFactory {
    PluginClassLoader create(Collection<URL> flatJarUrls, ClassLoader parentClassLoader);

    PluginClassLoader createForNestedJar(
            final ClassLoader parentClassLoader, final URL oneNestedJarUrl);

    PluginClassLoader createForNestedJar(
            final ClassLoader parentClassLoader,
            final URL oneNestedJarUrl,
            final Collection<String> embeddedJarPathsInNestedJar);
}
