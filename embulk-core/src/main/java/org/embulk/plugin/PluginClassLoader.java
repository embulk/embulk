package org.embulk.plugin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class PluginClassLoader extends URLClassLoader {
    private PluginClassLoader(
            final ClassLoader parentClassLoader,
            final URL oneNestedJarFileUrl,
            final Collection<URL> flatJarUrls,
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources) {
        super(combineUrlsToArray(oneNestedJarFileUrl, flatJarUrls == null ? Collections.<URL>emptyList() : flatJarUrls),
              parentClassLoader);

        // Given |oneNestedJarFileUrl| should be "file:...". |this.oneNestedJarUrlBase| should be "jar:file:...".
        URL oneNestedJarUrlBaseBuilt = null;
        if (oneNestedJarFileUrl != null) {
            try {
                oneNestedJarUrlBaseBuilt = new URL("jar", "", -1, oneNestedJarFileUrl + "!/");
            } catch (MalformedURLException ex) {
                // TODO: Notify this to reporters as far as possible.
                System.err.println("FATAL: Invalid JAR file URL: " + oneNestedJarFileUrl.toString());
                ex.printStackTrace();
            }
        }
        this.oneNestedJarUrlBase = oneNestedJarUrlBaseBuilt;

        this.parentFirstPackagePrefixes = ImmutableList.copyOf(
                parentFirstPackages.stream().map(pkg -> pkg + ".").collect(Collectors.toList()));
        this.parentFirstResourcePrefixes = ImmutableList.copyOf(
                parentFirstResources.stream().map(pkg -> pkg + "/").collect(Collectors.toList()));
    }

    @Deprecated  // Constructing directly with the constructor is deprecated (no warnings). Use static creator methods.
    public PluginClassLoader(
            final Collection<URL> flatJarUrls,
            final ClassLoader parentClassLoader,
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources) {
        this(parentClassLoader, null, flatJarUrls, parentFirstPackages, parentFirstResources);
    }

    /**
     * Creates PluginClassLoader for plugins with dependency JARs flat on the file system, like Gem-based plugins.
     */
    public static PluginClassLoader createForFlatJars(
            final ClassLoader parentClassLoader,
            final Collection<URL> flatJarUrls,
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources) {
        return new PluginClassLoader(
                parentClassLoader,
                null,
                flatJarUrls,
                parentFirstPackages,
                parentFirstResources);
    }

    /**
     * Creates PluginClassLoader for plugins with dependency JARs embedded in the plugin JAR itself, and even external.
     *
     * @param parentClassLoader  the parent ClassLoader of this PluginClassLoader instance
     * @param oneNestedJarFileUrl  "file:" URL of the plugin JAR file
     * @param dependencyJarUrls  collection of "file:" URLs of dependency JARs out of the plugin JAR
     * @param parentFirstPackages  collection of package names that are to be loaded first before the plugin's
     * @param parentFirstResources  collection of resource names that are to be loaded first before the plugin's
     */
    public static PluginClassLoader createForNestedJar(
            final ClassLoader parentClassLoader,
            final URL oneNestedJarFileUrl,
            final Collection<URL> dependencyJarUrls,
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources) {
        return new PluginClassLoader(
                parentClassLoader,
                oneNestedJarFileUrl,
                dependencyJarUrls,
                parentFirstPackages,
                parentFirstResources);
    }

    /**
     * Adds the specified path to the list of URLs (for {@code URLClassLoader}) to search for classes and resources.
     *
     * It internally calls {@code URLClassLoader#addURL}.
     *
     * Some plugins (embulk-input-jdbc, for example) are calling this method to load external JAR files.
     *
     * @see <a href="https://github.com/embulk/embulk-input-jdbc/blob/ebfff0b249d507fc730c87e08b56e6aa492060ca/embulk-input-jdbc/src/main/java/org/embulk/input/jdbc/AbstractJdbcInputPlugin.java#L586-L595">embulk-input-jdbc</a>
     */
    public void addPath(Path path) {
        try {
            addUrl(path.toUri().toURL());
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public void addUrl(URL url) {
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
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/ClassLoader.html#loadClass(java.lang.String,%20boolean)">Oracle Java7's ClassLoader#loadClass</a>
     * @see <a href="http://hg.openjdk.java.net/jdk7u/jdk7u/jdk/file/jdk7u141-b02/src/share/classes/java/lang/ClassLoader.java">OpenJDK7's ClassLoader</a>
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
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
                    // Passing through intentionally.
                }
            }

            // If the class is "parent-first" (to be loaded by the parent at first), try this part at first.
            // If the class is "not parent-first" (not to be loaded by the parent at first), the above part runs first.
            try {
                return resolveClass(getParent().loadClass(name), resolve);
            } catch (ClassNotFoundException ignored) {
                // Passing through intentionally.
            }

            // If the class is "parent-first" (to be loaded by the parent at first), this part runs after the above.
            if (parentFirst) {
                return resolveClass(findClass(name), resolve);
            }

            throw new ClassNotFoundException(name);
        }
    }

    private Class<?> resolveClass(Class<?> clazz, boolean resolve) {
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    @Override
    public URL getResource(String name) {
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
    public Enumeration<URL> getResources(String name) throws IOException {
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

    private static URL[] combineUrlsToArray(final URL oneNestedJarFileUrl, final Collection<URL> flatJarUrls) {
        final int offset;
        final URL[] allDirectJarUrls;
        if (oneNestedJarFileUrl == null) {
            offset = 0;
            allDirectJarUrls = new URL[flatJarUrls.size()];
        } else {
            offset = 1;
            allDirectJarUrls = new URL[flatJarUrls.size() + 1];
            allDirectJarUrls[0] = oneNestedJarFileUrl;
        }
        int i = 0;
        for (final URL flatJarUrl : flatJarUrls) {
            allDirectJarUrls[i + offset] = flatJarUrl;
            ++i;
        }
        return allDirectJarUrls;
    }

    private boolean isParentFirstPackage(String name) {
        for (String pkg : parentFirstPackagePrefixes) {
            if (name.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    private boolean isParentFirstPath(String name) {
        for (String path : parentFirstResourcePrefixes) {
            if (name.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    private final URL oneNestedJarUrlBase;
    private final List<String> parentFirstPackagePrefixes;
    private final List<String> parentFirstResourcePrefixes;
}
