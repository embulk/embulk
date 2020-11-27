package org.embulk.plugin;

import java.net.URL;
import java.util.Collection;

public interface PluginClassLoaderFactory {
    PluginClassLoader create(Collection<URL> flatJarUrls, ClassLoader parentClassLoader);

    void clear();
}
