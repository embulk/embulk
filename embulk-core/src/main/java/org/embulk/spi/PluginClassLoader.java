package org.embulk.spi;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import com.google.common.collect.ImmutableList;
import org.jruby.Ruby;

public class PluginClassLoader
        extends URLClassLoader
{
    private static final String[] PARENT_FIRST_PACKAGES = new String[] {
        "org.embulk.",
        "io.airlift.slice.",
        "com.fasterxml.jackson.",
        "com.google.inject.",
        "org.jruby.",
        "javax.inject.",
        "java.",
    };

    public PluginClassLoader(Ruby pluginJRubyRuntime, List<URL> urls)
    {
        this(urls, pluginJRubyRuntime.getJRubyClassLoader());
    }

    public PluginClassLoader(List<URL> urls, ClassLoader parent)
    {
        super(urls.toArray(new URL[urls.size()]), parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException
    {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                return resolveClass(loadedClass, resolve);
            }

            boolean parentFirst = isInParentFirstPackage(name);
            if (!parentFirst) {
                try {
                    return resolveClass(findClass(name), resolve);
                } catch (ClassNotFoundException ignored) {
                }
            }

            try {
                return resolveClass(getParent().loadClass(name), resolve);
            } catch (ClassNotFoundException ignored) {
            }

            if (parentFirst) {
                return resolveClass(findClass(name), resolve);
            }

            throw new ClassNotFoundException(name);
        }
    }

    private Class<?> resolveClass(Class<?> clazz, boolean resolve)
    {
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    private boolean isInParentFirstPackage(String name)
    {
        for (String pkg : PARENT_FIRST_PACKAGES) {
            if (name.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }
}
