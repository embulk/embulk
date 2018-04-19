package org.embulk.plugin;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class PluginClassLoader extends URLClassLoader {
    private PluginClassLoader(
            final ClassLoader parentClassLoader,
            final URL oneNestedJarFileUrl,
            final Collection<String> embeddedJarPathsInNestedJar,
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

        if (embeddedJarPathsInNestedJar == null) {
            this.embeddedJarPathsInNestedJar = Collections.<String>emptyList();
        } else {
            this.embeddedJarPathsInNestedJar = Collections.unmodifiableCollection(embeddedJarPathsInNestedJar);
        }
        this.parentFirstPackagePrefixes = ImmutableList.copyOf(
                Iterables.transform(parentFirstPackages, new Function<String, String>() {
                        public String apply(String pkg) {
                            return pkg + ".";
                        }
                    }));
        this.parentFirstResourcePrefixes = ImmutableList.copyOf(
                Iterables.transform(parentFirstResources, new Function<String, String>() {
                        public String apply(String pkg) {
                            return pkg + "/";
                        }
                    }));
        this.accessControlContext = AccessController.getContext();
    }

    @Deprecated  // Constructing directly with the constructor is deprecated (no warnings). Use static creator methods.
    public PluginClassLoader(
            final Collection<URL> flatJarUrls,
            final ClassLoader parentClassLoader,
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources) {
        this(parentClassLoader, null, null, flatJarUrls, parentFirstPackages, parentFirstResources);
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
                null,
                flatJarUrls,
                parentFirstPackages,
                parentFirstResources);
    }

    /**
     * Creates PluginClassLoader for plugins with dependency JARs embedded in the plugin JAR itself.
     *
     * @param parentClassLoader  the parent ClassLoader of this PluginClassLoader instance
     * @param oneNestedJarFileUrl  "file:" URL of the plugin JAR file
     * @param embeddedJarPathsInNestedJar  collection of resource names of embedded dependency JARs in the plugin JAR
     * @param parentFirstPackages  collection of package names that are to be loaded first before the plugin's
     * @param parentFirstResources  collection of resource names that are to be loaded first before the plugin's
     */
    public static PluginClassLoader createForNestedJar(
            final ClassLoader parentClassLoader,
            final URL oneNestedJarFileUrl,
            final Collection<String> embeddedJarPathsInNestedJar,
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources) {
        return new PluginClassLoader(
                parentClassLoader,
                oneNestedJarFileUrl,
                embeddedJarPathsInNestedJar,
                null,
                parentFirstPackages,
                parentFirstResources);
    }

    /**
     * Creates PluginClassLoader for plugins with dependency JARs embedded in the plugin JAR itself, and even external.
     *
     * @param parentClassLoader  the parent ClassLoader of this PluginClassLoader instance
     * @param oneNestedJarFileUrl  "file:" URL of the plugin JAR file
     * @param embeddedJarPathsInNestedJar  collection of resource names of embedded dependency JARs in the plugin JAR
     * @param dependencyJarUrls  collection of "file:" URLs of dependency JARs out of the plugin JAR
     * @param parentFirstPackages  collection of package names that are to be loaded first before the plugin's
     * @param parentFirstResources  collection of resource names that are to be loaded first before the plugin's
     */
    public static PluginClassLoader createForNestedJar(
            final ClassLoader parentClassLoader,
            final URL oneNestedJarFileUrl,
            final Collection<String> embeddedJarPathsInNestedJar,
            final Collection<URL> dependencyJarUrls,
            final Collection<String> parentFirstPackages,
            final Collection<String> parentFirstResources) {
        return new PluginClassLoader(
                parentClassLoader,
                oneNestedJarFileUrl,
                embeddedJarPathsInNestedJar,
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
     * Finds a class defined by the given name from given JARs and JARs in the given JAR.
     *
     * Classes directly inthe given JARs are always prioritized. Only if no such a class is found
     * directly in the given JAR, it tries to find the class in JARs in the given JAR.
     */
    @Override
    protected Class<?> findClass(final String className) throws ClassNotFoundException {
        if (this.oneNestedJarUrlBase == null || this.embeddedJarPathsInNestedJar.isEmpty()) {
            // Multiple flat JARs -- Gem-based plugins, or Single JAR (JAR-based plugins) without any embedded JAR
            return super.findClass(className);
        } else {
            // Single nested JAR -- JAR-based plugins
            try {
                // Classes directly in the plugin JAR are always prioritized.
                return super.findClass(className);
            } catch (ClassNotFoundException directClassNotFoundException) {
                try {
                    return AccessController.doPrivileged(
                            new PrivilegedExceptionAction<Class<?>>() {
                                public Class<?> run() throws ClassNotFoundException {
                                    try {
                                        return defineClassFromEmbeddedJars(className);
                                    } catch (ClassNotFoundException | LinkageError | ClassCastException ex) {
                                        throw ex;
                                    } catch (Throwable ex) {
                                        // Resource found from JARs in the JAR, but failed to load it as a class.
                                        throw new ClassNotFoundException(className, ex);
                                    }
                                }
                            },
                            this.accessControlContext);
                } catch (PrivilegedActionException ex) {
                    final Throwable internalException = ex.getException();
                    if (internalException instanceof ClassNotFoundException) {
                        throw (ClassNotFoundException) internalException;
                    }
                    if (internalException instanceof LinkageError) {
                        throw (LinkageError) internalException;
                    }
                    if (internalException instanceof ClassCastException) {
                        throw (ClassCastException) internalException;
                    }
                    throw new ClassNotFoundException(className, ex);
                }
            }
        }
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

    /**
     * Finds a resource recognized as the given name from given JARs and JARs in the given JAR.
     *
     * Resources directly in the given JARs are always prioritized. Only if no such a resource is found
     * directly in the given JAR, it tries to find the resource in JARs in the given JAR.
     *
     * Note that URLClassLoader#findResource is public while ClassLoader#findResource is protected.
     */
    @Override
    public URL findResource(final String resourceName) {
        if (this.oneNestedJarUrlBase == null || this.embeddedJarPathsInNestedJar.isEmpty()) {
            // Multiple flat JARs -- Gem-based plugins, or Single JAR (JAR-based plugins) without any embedded JAR
            return super.findResource(resourceName);
        } else {
            // Single nested JAR -- JAR-based plugins
            // Classes directly in the plugin JAR are always prioritized.
            final URL rootUrl = super.findResource(resourceName);
            if (rootUrl != null) {
                return rootUrl;
            }

            try {
                return AccessController.doPrivileged(
                        new PrivilegedExceptionAction<URL>() {
                            public URL run() {
                                return findResourceFromEmbeddedJars(resourceName);
                            }
                        },
                        this.accessControlContext);
            } catch (PrivilegedActionException ignored) {
                // Passing through intentionally.
            }

            return null;
        }
    }

    /**
     * Finds resources recognized as the given name from given JARs and JARs in the given JAR.
     *
     * Resources directly in the given JARs precede. Resources in JARs in the given JAR follow resources
     * directly in the given JARs.
     *
     * Note that URLClassLoader#findResources is public while ClassLoader#findResources is protected.
     */
    @Override
    public Enumeration<URL> findResources(final String resourceName) throws IOException {
        if (this.oneNestedJarUrlBase == null || this.embeddedJarPathsInNestedJar.isEmpty()) {
            // Multiple flat JARs -- Gem-based plugins, or Single JAR (JAR-based plugins) without any embedded JAR
            return super.findResources(resourceName);
        } else {
            // Single nested JAR -- JAR-based plugins
            final Vector<URL> urls = new Vector<URL>();

            // Classes directly in the plugin JAR are always prioritized.
            // Note that |super.findResources| may throw IOException.
            for (final Enumeration<URL> rootUrls = super.findResources(resourceName); rootUrls.hasMoreElements(); ) {
                urls.add(rootUrls.nextElement());
            }

            try {
                final List<URL> childUrls = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<List<URL>>() {
                            public List<URL> run() throws IOException {
                                return findResourcesFromEmbeddedJars(resourceName);
                            }
                        },
                        this.accessControlContext);
                urls.addAll(childUrls);
            } catch (PrivilegedActionException ignored) {
                // Passing through intentionally.
            }

            return urls.elements();
        }
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
    public InputStream getResourceAsStream(final String resourceName) {
        final boolean childFirst = isParentFirstPath(resourceName);

        if (childFirst) {
            final InputStream childInputStream = getResourceAsStreamFromChild(resourceName);
            if (childInputStream != null) {
                return childInputStream;
            }
        }

        final InputStream parentInputStream = getParent().getResourceAsStream(resourceName);
        if (parentInputStream != null) {
            return parentInputStream;
        }

        if (!childFirst) {
            final InputStream childInputStream = getResourceAsStreamFromChild(resourceName);
            if (childInputStream != null) {
                return childInputStream;
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

    /**
     * URLStreamHandler to handle resources in embedded JARs in the plugin JAR.
     */
    private static class PluginClassUrlStreamHandler extends URLStreamHandler {
        public PluginClassUrlStreamHandler(final String protocol) {
            this.protocol = protocol;
        }

        @Override
        protected URLConnection openConnection(final URL url) throws IOException {
            // Note that declaring variables here may cause unexpected behaviors.
            // https://stackoverflow.com/questions/9952815/s3-java-client-fails-a-lot-with-premature-end-of-content-length-delimited-messa
            return new URLConnection(url) {
                @Override
                public void connect() {}

                @Override
                public InputStream getInputStream() throws IOException {
                    final URL embulkPluginJarUrl = getURL();
                    if (!embulkPluginJarUrl.getProtocol().equals(protocol)) {
                        return null;
                    }
                    final String[] embulkPluginJarUrlSeparate = embulkPluginJarUrl.getPath().split("!!/");
                    if (embulkPluginJarUrlSeparate.length != 2) {
                        return null;
                    }
                    final URL embeddedJarUrl = new URL(embulkPluginJarUrlSeparate[0]);
                    final String embeddedResourceName = embulkPluginJarUrlSeparate[1];

                    final JarURLConnection embeddedJarUrlConnection;
                    try {
                        final URLConnection urlConnection = embeddedJarUrl.openConnection();
                        embeddedJarUrlConnection = (JarURLConnection) urlConnection;
                    } catch (ClassCastException ex) {
                        return null;
                    }

                    final JarInputStream embeddedJarInputStream =
                            new JarInputStream(embeddedJarUrlConnection.getInputStream());

                    // Note that |JarInputStream.getNextJarEntry| may throw IOException.
                    JarEntry jarEntry = embeddedJarInputStream.getNextJarEntry();
                    while (jarEntry != null) {
                        if (jarEntry.getName().equals(embeddedResourceName)) {
                            return embeddedJarInputStream;  // The InputStream points the specific "JAR entry".
                        }
                        // Note that |JarInputStream.getNextJarEntry| may throw IOException.
                        jarEntry = embeddedJarInputStream.getNextJarEntry();
                    }
                    return null;
                }
            };
        }

        public final String protocol;
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

    /**
     * Defines a class with given class name from JARs embedded in the plugin JAR.
     *
     * It tries to continue even if Exceptions are throws in one of embedded JARs so that
     * it can find the target class without affected from other unrelated JARs.
     */
    private Class<?> defineClassFromEmbeddedJars(final String className)
            throws ClassNotFoundException {
        final String classResourceName = className.replace('.', '/').concat(".class");

        Throwable lastException = null;
        // TODO: Speed up class loading by caching?
        for (final String embeddedJarPath : this.embeddedJarPathsInNestedJar) {
            final URL embeddedJarUrl;
            final JarURLConnection embeddedJarUrlConnection;
            final JarInputStream embeddedJarInputStream;
            try {
                embeddedJarUrl = getEmbeddedJarUrl(embeddedJarPath);
                embeddedJarUrlConnection = getEmbeddedJarUrlConnection(embeddedJarUrl);
                embeddedJarInputStream = getEmbeddedJarInputStream(embeddedJarUrlConnection);
            } catch (IOException ex) {
                lastException = ex;
                continue;
            }

            final Manifest manifest;
            try {
                manifest = embeddedJarUrlConnection.getManifest();
            } catch (IOException ex) {
                // TODO: Notify this to reporters as far as possible.
                lastException = ex;
                System.err.println("Failed to load manifest in embedded JAR: " + embeddedJarPath);
                ex.printStackTrace();
                continue;
            }

            JarEntry jarEntry;
            try {
                jarEntry = embeddedJarInputStream.getNextJarEntry();
            } catch (IOException ex) {
                // TODO: Notify this to reporters as far as possible.
                lastException = ex;
                System.err.println("Failed to load entry in embedded JAR: " + embeddedJarPath);
                ex.printStackTrace();
                continue;
            }
            jarEntries:
            while (jarEntry != null) {
                if (jarEntry.getName().equals(classResourceName)) {
                    final int lastDotIndexInClassName = className.lastIndexOf('.');
                    final String packageName;
                    if (lastDotIndexInClassName != -1) {
                        packageName = className.substring(0, lastDotIndexInClassName);
                    } else {
                        packageName = null;
                    }

                    final URL codeSourceUrl = embeddedJarUrl;

                    // Define the package if the package is not loaded / defined yet.
                    if (packageName != null) {
                        // TODO: Consider package sealing.
                        // https://docs.oracle.com/javase/tutorial/deployment/jar/sealman.html
                        if (this.getPackage(packageName) == null) {
                            try {
                                final Attributes fileAttributes;
                                final Attributes mainAttributes;
                                if (manifest != null) {
                                    fileAttributes = manifest.getAttributes(classResourceName);
                                    mainAttributes = manifest.getMainAttributes();
                                } else {
                                    fileAttributes = null;
                                    mainAttributes = null;
                                }

                                this.definePackage(
                                        packageName,
                                        getAttributeFromAttributes(mainAttributes, fileAttributes, classResourceName,
                                                                   Attributes.Name.SPECIFICATION_TITLE),
                                        getAttributeFromAttributes(mainAttributes, fileAttributes, classResourceName,
                                                                   Attributes.Name.SPECIFICATION_VERSION),
                                        getAttributeFromAttributes(mainAttributes, fileAttributes, classResourceName,
                                                                   Attributes.Name.SPECIFICATION_VENDOR),
                                        getAttributeFromAttributes(mainAttributes, fileAttributes, classResourceName,
                                                                   Attributes.Name.IMPLEMENTATION_TITLE),
                                        getAttributeFromAttributes(mainAttributes, fileAttributes, classResourceName,
                                                                   Attributes.Name.IMPLEMENTATION_VERSION),
                                        getAttributeFromAttributes(mainAttributes, fileAttributes, classResourceName,
                                                                   Attributes.Name.IMPLEMENTATION_VENDOR),
                                        null);
                            } catch (IllegalArgumentException ex) {
                                // The package duplicates -- in parallel cases
                                if (getPackage(packageName) == null) {
                                    // TODO: Notify this to reporters as far as possible.
                                    lastException = ex;
                                    System.err.println("FATAL: Should not happen. Package duplicated: " + packageName);
                                    ex.printStackTrace();
                                    continue;
                                }
                            }
                        }
                    }

                    final long classResourceSize = jarEntry.getSize();
                    final byte[] classResourceBytes;
                    final long actualSize;

                    if (classResourceSize > -1) {  // JAR entry size available
                        classResourceBytes = new byte[(int) classResourceSize];
                        try {
                            actualSize = embeddedJarInputStream.read(classResourceBytes, 0, (int) classResourceSize);
                        } catch (IOException ex) {
                            // TODO: Notify this to reporters as far as possible.
                            lastException = ex;
                            System.err.println("Failed to load entry in embedded JAR: " + classResourceName);
                            ex.printStackTrace();
                            break jarEntries;  // Breaking from loading since this JAR looks broken.
                        }
                        if (actualSize != classResourceSize) {
                            // TODO: Notify this to reporters as far as possible.
                            System.err.println("Broken entry in embedded JAR: " + classResourceName);
                            break jarEntries;  // Breaking from loading since this JAR looks broken.
                        }
                    } else {  // JAR entry size unavailable
                        final ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
                        final byte[] buffer = new byte[1024];
                        long accumulatedSize = 0;
                        while (true) {
                            final long readSize;
                            try {
                                readSize = embeddedJarInputStream.read(buffer, 0, 1024);
                            } catch (IOException ex) {
                                // TODO: Notify this to reporters as far as possible.
                                lastException = ex;
                                System.err.println("Failed to load entry in embedded JAR: " + classResourceName);
                                ex.printStackTrace();
                                break jarEntries;  // Breaking from loading since this JAR looks broken.
                            }

                            if (readSize < 0) {
                                break;
                            }

                            bytesStream.write(buffer, 0, (int) readSize);
                            accumulatedSize += readSize;
                        }
                        actualSize = accumulatedSize;
                        classResourceBytes = bytesStream.toByteArray();
                    }

                    final CodeSource codeSource = new CodeSource(codeSourceUrl, (CodeSigner[]) null);
                    final Class<?> definedClass;
                    try {
                        definedClass = defineClass(className, classResourceBytes, 0, (int) actualSize, codeSource);
                    } catch (Throwable ex) {
                        // TODO: Notify this to reporters as far as possible.
                        lastException = ex;
                        System.err.println("Failed to load entry in embedded JAR: " + classResourceName);
                        ex.printStackTrace();
                        continue;
                    }
                    return definedClass;
                }
                try {
                    jarEntry = embeddedJarInputStream.getNextJarEntry();
                } catch (IOException ex) {
                    // TODO: Notify this to reporters as far as possible.
                    lastException = ex;
                    System.err.println("Failed to load entry in embedded JAR: " + classResourceName);
                    ex.printStackTrace();
                    break jarEntries;  // Breaking from loading since this JAR looks broken.
                }
            }
        }
        if (lastException != null) {
            throw new ClassNotFoundException(className, lastException);
        } else {
            throw new ClassNotFoundException(className);
        }
    }

    private InputStream getResourceAsStreamFromChild(final String resourceName) {
        if (this.oneNestedJarUrlBase == null || this.embeddedJarPathsInNestedJar.isEmpty()) {
            // Multiple flat JARs -- Gem-based plugins, or Single JAR (JAR-based plugins) without any embedded JAR
            return super.getResourceAsStream(resourceName);
        } else {
            // Single nested JAR -- JAR-based plugins
            // Resources directly in the plugin JAR are prioritized.
            final InputStream inputStream = super.getResourceAsStream(resourceName);
            if (inputStream == null) {
                try {
                    final InputStream childInputStream = AccessController.doPrivileged(
                            new PrivilegedExceptionAction<InputStream>() {
                                public InputStream run() {
                                    return getResourceAsStreamFromEmbeddedJars(resourceName);
                                }
                            },
                            this.accessControlContext);
                    if (childInputStream != null) {
                        return childInputStream;
                    }
                } catch (PrivilegedActionException ignored) {
                    // Passing through intentionally.
                }
            }
        }
        return null;
    }

    private InputStream getResourceAsStreamFromEmbeddedJars(final String resourceName) {
        for (final String embeddedJarPath : this.embeddedJarPathsInNestedJar) {
            final JarInputStream embeddedJarInputStream;
            try {
                embeddedJarInputStream = getEmbeddedJarInputStream(embeddedJarPath);
            } catch (IOException ex) {
                // TODO: Notify this to reporters as far as possible.
                System.err.println("Failed to load entry in embedded JAR: " + resourceName + " / " + embeddedJarPath);
                ex.printStackTrace();
                continue;
            }

            JarEntry jarEntry;
            try {
                jarEntry = embeddedJarInputStream.getNextJarEntry();
            } catch (IOException ex) {
                // TODO: Notify this to reporters as far as possible.
                System.err.println("Failed to load entry in embedded JAR: " + resourceName + " / " + embeddedJarPath);
                ex.printStackTrace();
                continue;
            }
            while (jarEntry != null) {
                if (jarEntry.getName().equals(resourceName)) {
                    return embeddedJarInputStream;  // Pointing the specific "JAR entry"
                }
            }
        }
        return null;
    }

    private URL findResourceFromEmbeddedJars(final String resourceName) {
        for (final String embeddedJarPath : this.embeddedJarPathsInNestedJar) {
            final URL embeddedJarUrl;
            final JarURLConnection embeddedJarUrlConnection;
            final JarInputStream embeddedJarInputStream;
            try {
                embeddedJarUrl = getEmbeddedJarUrl(embeddedJarPath);
                embeddedJarUrlConnection = getEmbeddedJarUrlConnection(embeddedJarUrl);
                embeddedJarInputStream = getEmbeddedJarInputStream(embeddedJarUrlConnection);
            } catch (IOException ex) {
                // TODO: Notify this to reporters as far as possible.
                System.err.println("Failed to load entry in embedded JAR: " + resourceName + " / " + embeddedJarPath);
                ex.printStackTrace();
                continue;
            }

            JarEntry jarEntry;
            try {
                jarEntry = embeddedJarInputStream.getNextJarEntry();
            } catch (IOException ex) {
                // TODO: Notify this to reporters as far as possible.
                System.err.println("Failed to load entry in embedded JAR: " + resourceName + " / " + embeddedJarPath);
                ex.printStackTrace();
                continue;
            }
            jarEntries:
            while (jarEntry != null) {
                if (jarEntry.getName().equals(resourceName)) {
                    // For resources (not classes) in nested JARs, the schema and the URL should be like:
                    // "embulk-plugin-jar:jar:file://.../plugin.jar!/classpath/library.jar!!/org.library/resource.txt"
                    //
                    // The "embulk-plugin-jar" URL is processed with |PluginClassUrlStreamHandler|.
                    // See also: https://www.ibm.com/developerworks/library/j-onejar/index.html
                    //
                    // The URL lives only in the JVM execution.
                    try {
                        // The URL lives only in the JVM execution.
                        return new URL("embulk-plugin-jar", "", -1, embeddedJarUrl + "!!/" + resourceName,
                                       new PluginClassUrlStreamHandler("embulk-plugin-jar"));
                    } catch (MalformedURLException ex) {
                        // TODO: Notify this to reporters as far as possible.
                        System.err.println("Failed to load entry in embedded JAR: " + resourceName + " / " + embeddedJarPath);
                        ex.printStackTrace();
                        break jarEntries;
                    }
                }
                try {
                    jarEntry = embeddedJarInputStream.getNextJarEntry();
                } catch (IOException ex) {
                    // TODO: Notify this to reporters as far as possible.
                    System.err.println("Failed to load entry in embedded JAR: " + resourceName + " / " + embeddedJarPath);
                    ex.printStackTrace();
                    break jarEntries;
                }
            }
        }
        return null;
    }

    private List<URL> findResourcesFromEmbeddedJars(final String resourceName) throws IOException {
        final ArrayList<URL> resourceUrls = new ArrayList<URL>();
        for (final String embeddedJarPath : this.embeddedJarPathsInNestedJar) {
            final URL embeddedJarUrl = getEmbeddedJarUrl(embeddedJarPath);
            final JarURLConnection embeddedJarUrlConnection = getEmbeddedJarUrlConnection(embeddedJarUrl);
            final JarInputStream embeddedJarInputStream = getEmbeddedJarInputStream(embeddedJarUrlConnection);

            // Note that |JarInputStream.getNextJarEntry| may throw IOException.
            JarEntry jarEntry = embeddedJarInputStream.getNextJarEntry();
            while (jarEntry != null) {
                if (jarEntry.getName().equals(resourceName)) {
                    // For resources (not classes) in nested JARs, the schema and the URL should be like:
                    // "embulk-plugin-jar:jar:file://.../plugin.jar!/classpath/library.jar!/org.library/resource.txt"
                    //
                    // The "embulk-plugin-jar" URL is processed with |PluginClassUrlStreamHandler|.
                    // See also: https://www.ibm.com/developerworks/library/j-onejar/index.html
                    //
                    // The URL lives only in the JVM execution.
                    //
                    // Note that |new URL| may throw MalformedURLException (extending IOException).
                    resourceUrls.add(new URL("embulk-plugin-jar", "", -1, embeddedJarUrl + "!!/" + resourceName,
                                             new PluginClassUrlStreamHandler("embulk-plugin-jar")));
                }
                // Note that |JarInputStream.getNextJarEntry| may throw IOException.
                jarEntry = embeddedJarInputStream.getNextJarEntry();
            }
        }
        return resourceUrls;
    }

    private URL getEmbeddedJarUrl(final String embeddedJarPath) throws MalformedURLException {
        final URL embeddedJarUrl;
        try {
            embeddedJarUrl = new URL(this.oneNestedJarUrlBase, embeddedJarPath);
        } catch (MalformedURLException ex) {
            // TODO: Notify this to reporters as far as possible.
            System.err.println("Failed to load entry in embedded JAR: " + embeddedJarPath);
            ex.printStackTrace();
            throw ex;
        }
        return embeddedJarUrl;
    }

    private JarURLConnection getEmbeddedJarUrlConnection(final URL embeddedJarUrl) throws IOException {
        final JarURLConnection embeddedJarUrlConnection;
        try {
            final URLConnection urlConnection = embeddedJarUrl.openConnection();
            embeddedJarUrlConnection = (JarURLConnection) urlConnection;
        } catch (IOException ex) {
            // TODO: Notify this to reporters as far as possible.
            System.err.println("Failed to load entry in embedded JAR: " + embeddedJarUrl.toString());
            ex.printStackTrace();
            throw ex;
        }
        return embeddedJarUrlConnection;
    }

    private JarInputStream getEmbeddedJarInputStream(final JarURLConnection embeddedJarUrlConnection) throws IOException {
        final JarInputStream embeddedJarInputStream;
        try {
            final InputStream inputStream = embeddedJarUrlConnection.getInputStream();
            embeddedJarInputStream = new JarInputStream(inputStream);
        } catch (IOException ex) {
            // TODO: Notify this to reporters as far as possible.
            System.err.println("Failed to load entry in embedded JAR: " + embeddedJarUrlConnection.toString());
            ex.printStackTrace();
            throw ex;
        }
        return embeddedJarInputStream;
    }

    private JarInputStream getEmbeddedJarInputStream(final String embeddedJarPath)
            throws IOException {
        final URL embeddedJarUrl = getEmbeddedJarUrl(embeddedJarPath);
        final JarURLConnection embeddedJarUrlConnection = getEmbeddedJarUrlConnection(embeddedJarUrl);
        return getEmbeddedJarInputStream(embeddedJarUrlConnection);
    }

    private static String getAttributeFromAttributes(
            final Attributes mainAttributes,
            final Attributes fileAttributes,
            final String classResourceName,
            final Attributes.Name attributeName) {
        if (fileAttributes != null) {
            final String value = fileAttributes.getValue(attributeName);
            if (value != null) {
                return value;
            }
        }
        if (mainAttributes != null) {
            final String value = mainAttributes.getValue(attributeName);
            if (value != null) {
                return value;
            }
        }
        return null;
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
    private final Collection<String> embeddedJarPathsInNestedJar;
    private final List<String> parentFirstPackagePrefixes;
    private final List<String> parentFirstResourcePrefixes;
    private final AccessControlContext accessControlContext;
}
