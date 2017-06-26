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

    /**
     * Adds the specified path to the list of URLs (for {@code URLClassLoader}) to search for classes and resources.
     *
     * It internally calls {@code URLClassLoader#addURL}.
     *
     * Some plugins (embulk-input-jdbc, for example) are calling this method to load external JAR files.
     *
     * @see https://github.com/embulk/embulk-input-jdbc/blob/ebfff0b249d507fc730c87e08b56e6aa492060ca/embulk-input-jdbc/src/main/java/org/embulk/input/jdbc/AbstractJdbcInputPlugin.java#L586-L595
     */
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

    /**
     * Loads the class with the specified binary name prioritized by the "parent-first" condition.
     *
     * It copy-cats {@code ClassLoader#loadClass} while the "parent-first" priorities are considered.
     *
     * If the specified class is "parent-first", it behaves the same as {@code ClassLoader#loadClass} ordered as below.
     *
     * <ol>
     *
     * <li><p>Invoke the {@code #findLoadedClass} method to check if the class has already been loaded.</p></li>
     *
     * <li><p>Invoke the parent's {@code #loadClass} method.
     *
     * <li><p>Invoke the {@code #findClass} method of this class loader to find the class.</p></li>
     *
     * </ol>
     *
     * If the specified class is "NOT parent-first", the 2nd and 3rd actions are swapped.
     *
     * @see https://docs.oracle.com/javase/7/docs/api/java/lang/ClassLoader.html#loadClass(java.lang.String,%20boolean)
     * @see http://hg.openjdk.java.net/jdk7u/jdk7u/jdk/file/jdk7u141-b02/src/share/classes/java/lang/ClassLoader.java
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException
    {
        synchronized (getClassLoadingLock(name)) {
            // If the class has already been loaded by this {@code ClassLoader} or the parent's {@code ClassLoader},
            // find the loaded class and return it.
            final Class<?> loadedClass = findLoadedClass(name);

            if (loadedClass != null) {
                return resolveClass(loadedClass, resolve);
            }

            final boolean parentFirst = isParentFirstPackage(name);

            // If the class is "not parent-first" (not to be loaded by the parent at first),
            // try {@code #findClass} of the child's ({@code PluginClassLoader}'s).
            if (!parentFirst) {
                try {
                    return resolveClass(findClass(name), resolve);
                } catch (ClassNotFoundException ignored) {
                }
            }

            // If the class is "parent-first" (to be loaded by the parent at first), try this part at first.
            // If the class is "not parent-first" (not to be loaded by the parent at first), the above part runs first.
            try {
                return resolveClass(getParent().loadClass(name), resolve);
            } catch (ClassNotFoundException ignored) {
            }

            // If the class is "parent-first" (to be loaded by the parent at first), this part runs after the above.
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
