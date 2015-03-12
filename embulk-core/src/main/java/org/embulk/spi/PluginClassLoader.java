package org.embulk.spi;

import java.nio.file.Path;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.util.List;
import com.google.common.collect.ImmutableList;
import org.jruby.Ruby;

public class PluginClassLoader
        extends URLClassLoader
{
    private static final String[] CHILD_FIRST_PACKAGES = new String[] {
        "io.netty.",
        "org.yaml.",
        "com.ibm.icu.",
    };

    public PluginClassLoader(Ruby pluginJRubyRuntime, List<URL> urls)
    {
        this(urls, pluginJRubyRuntime.getJRubyClassLoader());
    }

    public PluginClassLoader(List<URL> urls, ClassLoader parent)
    {
        super(urls.toArray(new URL[urls.size()]), parent);
    }

    public void addPath(Path path)
    {
        try {
            addUrl(path.toUri().toURL());
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public void addUrl(URL url)
    {
        super.addURL(url);
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

            boolean childFirst = isInChildFirstPackage(name);
            if (childFirst) {
                try {
                    return resolveClass(findClass(name), resolve);
                } catch (ClassNotFoundException ignored) {
                }
            }

            try {
                return resolveClass(getParent().loadClass(name), resolve);
            } catch (ClassNotFoundException ignored) {
            }

            if (!childFirst) {
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

    private boolean isInChildFirstPackage(String name)
    {
        for (String pkg : CHILD_FIRST_PACKAGES) {
            if (name.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }
}
