package org.embulk.plugin;

import java.util.Collection;
import java.net.URL;
import java.security.PermissionCollection;

public interface PluginClassLoaderFactory
{
    PluginClassLoader create(Collection<URL> urls, ClassLoader parentClassLoader);
    PluginClassLoader create(Collection<URL> urls, ClassLoader parentClassLoader, PermissionCollection permissions);
}
