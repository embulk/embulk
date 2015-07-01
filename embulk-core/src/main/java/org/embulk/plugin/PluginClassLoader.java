package org.embulk.plugin;

import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Enumeration;
import java.io.IOException;
import java.nio.file.Path;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

public class PluginClassLoader
        extends URLClassLoader
{
    private final List<String> parentFirstPackagePrefixes;
    private final List<String> parentFirstResourcePrefixes;

    public PluginClassLoader(Collection<URL> urls, ClassLoader parent,
            Collection<String> parentFirstPackages,
            Collection<String> parentFirstResources)
    {
        super(urls.toArray(new URL[urls.size()]), parent);
        this.parentFirstPackagePrefixes = ImmutableList.copyOf(
                Iterables.transform(parentFirstPackages, new Function<String, String>() {
                    public String apply(String pkg)
                    {
                        return pkg + ".";
                    }
                }));
        this.parentFirstResourcePrefixes = ImmutableList.copyOf(
                Iterables.transform(parentFirstResources, new Function<String, String>() {
                    public String apply(String pkg)
                    {
                        return pkg + "/";
                    }
                }));
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

            boolean parentFirst = isParentFirstPackage(name);
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

    @Override
    public URL getResource(String name)
    {
        boolean childFirst = isParentFirstPath(name);

        if (childFirst) {
            URL childUrl = findResource(name);
            if (childUrl != null) {
                return childUrl;
            }
        }

        URL parentUrl = getParent().getResource(name);
        if (parentUrl != null) {
            return parentUrl;
        }

        if (!childFirst) {
            URL childUrl = findResource(name);
            if (childUrl != null) {
                return childUrl;
            }
        }

        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name)
            throws IOException
    {
        List<Iterator<URL>> resources = new ArrayList<>();

        boolean parentFirst = isParentFirstPath(name);

        if (!parentFirst) {
            Iterator<URL> childResources = Iterators.forEnumeration(findResources(name));
            resources.add(childResources);
        }

        Iterator<URL> parentResources = Iterators.forEnumeration(getParent().getResources(name));
        resources.add(parentResources);

        if (parentFirst) {
            Iterator<URL> childResources = Iterators.forEnumeration(findResources(name));
            resources.add(childResources);
        }

        return Iterators.asEnumeration(Iterators.concat(resources.iterator()));
    }

    private boolean isParentFirstPackage(String name)
    {
        for (String pkg : parentFirstPackagePrefixes) {
            if (name.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    private boolean isParentFirstPath(String name)
    {
        for (String path : parentFirstResourcePrefixes) {
            if (name.startsWith(path)) {
                return true;
            }
        }
        return false;
    }
}
