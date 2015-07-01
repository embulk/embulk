package org.embulk.plugin;

import java.util.Collection;
import java.net.URL;

public interface PluginClassLoaderFactory
{
    public PluginClassLoader create(Collection<URL> urls, ClassLoader parentClassLoader);
}
