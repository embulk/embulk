package org.embulk.deps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
// import java.security.AccessController;
// import java.security.PrivilegedExceptionAction;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/*
 * It should not implement |loadClass| by itself.
 */
class SelfContainedDependencyClassLoader extends DependencyClassLoader {
    private SelfContainedDependencyClassLoader(
            final ClassLoader containerJarClassLoader,
            final List<String> jarResourceNames,
            final Map<String, ResourceContent> resourceContents,
            final Map<String, Manifest> manifests,
            final Collection<Throwable> exceptions) {
        super(containerJarClassLoader);
        this.containerJarClassLoader = containerJarClassLoader;
        this.jarResourceNames = jarResourceNames;
        this.resourceContents = resourceContents;
        this.manifests = manifests;
        this.exceptions = exceptions;
    }

    static SelfContainedDependencyClassLoader of(
            final ClassLoader containerJarClassLoader,
            final List<String> jarResourceNames)
            throws IOException, UnacceptableDuplicatedResourceException {
        final ExtractedContents extracted = extractContainedJars(containerJarClassLoader, jarResourceNames);
        return new SelfContainedDependencyClassLoader(
                containerJarClassLoader,
                jarResourceNames,
                extracted.resources,
                extracted.manifests,
                extracted.exceptions);
    }

    // This |findClass| should not be called when the class has already been loaded.
    // The default |loadClass| checks if the class has already been loaded before calling |findClass|.
    /*
    @Override
    protected Class<?> findClass(final String className) throws ClassNotFoundException {
        try {
            // Classes directly contained in the container JAR are prioritized.
            return this.containerJarClassLoader.findClass(className);
        } catch (ClassNotFoundException directClassNotFoundException) {
            try {
                return AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Class<?>>() {
                        @Override
                        public Class<?> run() throws ClassNotFoundException {
                            try {
                                return defineClassFromLoadedResources(className);
                            } catch (ClassNotFoundException | LinkageError | ClassCastException ex) {
                                throw ex;
                            } catch (Throwable ex) {
                                // Found a resource in the container JAR, but failed to load it as a class.
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
    */

    private Class<?> defineClassFromExtractedResources(final String className) throws IOException {
        final String resourceName = className.replace('.', '/').concat(".class");
        final int indexLastPeriod = className.lastIndexOf('.');

        if (indexLastPeriod != -1) {
            final String packageName = className.substring(0, indexLastPeriod);
            final ResourceContent resource = this.resourceContents.get(resourceName);
            final Manifest manifest = resource.manifest;

            // Check if package already loaded.
            /*
            if (getAndVerifyPackage(packageName, manifest, url) == null) {
                try {
                    if (man != null) {
                        definePackage(pkgname, man, url);
                    } else {
                        definePackage(pkgname, null, null, null, null, null, null, null);
                    }
                } catch (IllegalArgumentException iae) {
                    // parallel-capable class loaders: re-verify in case of a
                    // race condition
                    if (getAndVerifyPackage(pkgname, man, url) == null) {
                        // Should never happen
                        throw new AssertionError("Cannot find package " +
                                                 pkgname);
                    }
                }
            }
            */
        }
        /*
        URL url = res.getCodeSourceURL();

        // Now read the class bytes and define the class
        java.nio.ByteBuffer bb = res.getByteBuffer();
        if (bb != null) {
            // Use (direct) ByteBuffer:
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            return defineClass(name, bb, cs);
        } else {
            byte[] b = res.getBytes();
            // must read certificates AFTER reading bytes.
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            return defineClass(name, b, 0, b.length, cs);
        }
        */
        return null;
    }

    private Package definePackageFromManifest(
            final String packageName,
            final Manifest manifest,
            final URL codeSourceUrl)
            throws IllegalArgumentException {
        // https://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Per-Entry_Attributes
        final Attributes perEntryAttributes = manifest.getAttributes(packageName.replace('.', '/').concat("/"));

        final Attributes mainAttributes = manifest.getMainAttributes();

        return this.definePackage(
                packageName,
                (String) perEntryAttributes.getOrDefault(
                        Attributes.Name.SPECIFICATION_TITLE,
                        mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE)),
                (String) perEntryAttributes.getOrDefault(
                        Attributes.Name.SPECIFICATION_VERSION,
                        mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION)),
                (String) perEntryAttributes.getOrDefault(
                        Attributes.Name.SPECIFICATION_VENDOR,
                        mainAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR)),
                (String) perEntryAttributes.getOrDefault(
                        Attributes.Name.IMPLEMENTATION_TITLE,
                        mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE)),
                (String) perEntryAttributes.getOrDefault(
                        Attributes.Name.IMPLEMENTATION_VERSION,
                        mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION)),
                (String) perEntryAttributes.getOrDefault(
                        Attributes.Name.IMPLEMENTATION_VENDOR,
                        mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR)),
                "true".equalsIgnoreCase(
                        (String) (perEntryAttributes.getOrDefault(
                                      Attributes.Name.SEALED,
                                      mainAttributes.getValue(Attributes.Name.SEALED))))
                        ? codeSourceUrl
                        : null);
    }

    private static class ExtractedContents {
        private ExtractedContents(
                final Map<String, ResourceContent> resources,
                final Map<String, Manifest> manifests,
                final Collection<Throwable> exceptions) {
            this.resources = resources;
            this.manifests = manifests;
            this.exceptions = exceptions;
        }

        public final Map<String, ResourceContent> resources;
        public final Map<String, Manifest> manifests;
        public final Collection<Throwable> exceptions;
    }

    private static class ResourceContent {
        private ResourceContent(final byte[] bytes, final Manifest manifest) {
            this.bytes = bytes;
            this.manifest = manifest;
            this.origins = Collections.emptyMap();
        }

        private ResourceContent(final LinkedHashMap<String, byte[]> origins) {
            this.bytes = null;
            this.manifest = null;
            this.origins = Collections.unmodifiableMap(origins);
        }

        private ResourceContent(final String origin, final byte[] bytes) {
            final LinkedHashMap<String, byte[]> originsBuilt = new LinkedHashMap<>();
            originsBuilt.put(origin, bytes);
            this.bytes = null;
            this.manifest = null;
            this.origins = Collections.unmodifiableMap(originsBuilt);
        }

        private ResourceContent withAnotherOrigin(final String origin, final byte[] bytes) {
            final LinkedHashMap<String, byte[]> originsBuilt = new LinkedHashMap<>(this.origins);
            // TODO: Check duplication of origin.
            originsBuilt.put(origin, bytes);
            return new ResourceContent(originsBuilt);
        }

        private final byte[] bytes;
        private final Manifest manifest;
        private final Map<String, byte[]> origins;
    }

    private static ExtractedContents extractContainedJars(
            final ClassLoader containerJarClassLoader,
            final List<String> jarResourceNames)
            throws IOException, UnacceptableDuplicatedResourceException {
        final HashMap<String, ResourceContent> resourceContents = new HashMap<>();
        final LinkedHashMap<String, Manifest> manifestsBuilt = new LinkedHashMap<>();
        final ArrayList<Throwable> exceptions = new ArrayList<>();

        for (final String jarResourceName : jarResourceNames) {
            final InputStream inputStream = containerJarClassLoader.getResourceAsStream(jarResourceName);
            if (inputStream == null) {
                exceptions.add(new Exception(String.format("%s is not contained.", jarResourceName)));
                continue;
            }

            final JarInputStream jarInputStream;
            try {
                jarInputStream = new JarInputStream(inputStream, false);
            } catch (IOException ex) {
                exceptions.add(new Exception(String.format("%s is invalid.", jarResourceName)));
                continue;
            }
            final Manifest manifest = jarInputStream.getManifest();
            manifestsBuilt.put(jarResourceName, manifest);
            extractContainedJar(jarInputStream, jarResourceName, manifest, resourceContents);
        }

        return new ExtractedContents(Collections.unmodifiableMap(resourceContents),
                                  Collections.unmodifiableMap(manifestsBuilt),
                                  Collections.unmodifiableList(exceptions));
    }

    private static void extractContainedJar(
            final JarInputStream containedJarInputStream,
            final String jarResourceName,
            final Manifest manifest,
            final HashMap<String, ResourceContent> resourceContents)
            throws IOException, UnacceptableDuplicatedResourceException {
        final byte[] buffer = new byte[4096];

        JarEntry containedJarEntry;
        while ((containedJarEntry = (JarEntry) containedJarInputStream.getNextEntry()) != null) {
            if (containedJarEntry.isDirectory()) {
                continue;
            }
            final String entryName = containedJarEntry.getName();

            if (entryName.startsWith("META-INF")) {
                // Resources starting with "META-INF" can be duplicated.
                final byte[] resourceBytes = readAllBytes(containedJarInputStream, buffer);
                resourceContents.compute(
                        entryName,
                        (key, found) -> found == null
                                        ? new ResourceContent(jarResourceName, resourceBytes)
                                        : found.withAnotherOrigin(jarResourceName, resourceBytes));
            } else {
                // Resources not starting with "META-INF" are not allowed to duplicated.
                if (resourceContents.containsKey(entryName)) {
                    throw new UnacceptableDuplicatedResourceException(
                            String.format("FATAL: Duplicated resources in self-contained JARs: %s", entryName));
                }
                final byte[] resourceBytes = readAllBytes(containedJarInputStream, buffer);
                resourceContents.put(entryName, new ResourceContent(resourceBytes, manifest));
            }
        }
    }

    private static byte[] readAllBytes(final InputStream input, final byte[] buffer) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        while (true) {
            final int lengthRead = input.read(buffer);
            if (lengthRead < 0) {
                break;
            }
            result.write(buffer, 0, lengthRead);
        }
        return result.toByteArray();
    }

    /*
    private static class ByteCode {
        ByteCode(final String name,
                 final String original,
                 final byte[] bytes,
                 final String codebase,
                 final Manifest manifest) {
            this.name = name;
            this.original = original;
            this.bytes = bytes;
            this.codebase = codebase;
            this.manifest = manifest;
        }

        public final String name;
        public final String original;
        public final byte[] bytes;
        public final String codebase;
        public final Manifest manifest;
    }
    */

    /*
    @Override
    protected Class<?> findClass(final String className) throws ClassNotFoundException {
        try {
            // Classes directly contained in the container JAR are prioritized.
            return this.containerJarClassLoader.findClass(className);
        } catch (ClassNotFoundException directClassNotFoundException) {
            try {
                return AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Class<?>>() {
                        @Override
                        public Class<?> run() throws ClassNotFoundException {
                            try {
                                return defineClassFromEmbeddedJars(className);
                            } catch (ClassNotFoundException | LinkageError | ClassCastException ex) {
                                throw ex;
                            } catch (Throwable ex) {
                                // Found a resource in the container JAR, but failed to load it as a class.
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
    */

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
    /*
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
    */

    /**
     * Finds a resource recognized as the given name from given JARs and JARs in the given JAR.
     *
     * Resources directly in the given JARs are always prioritized. Only if no such a resource is found
     * directly in the given JAR, it tries to find the resource in JARs in the given JAR.
     *
     * Note that URLClassLoader#findResource is public while ClassLoader#findResource is protected.
     */
    /*
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
    */

    /**
     * Finds resources recognized as the given name from given JARs and JARs in the given JAR.
     *
     * Resources directly in the given JARs precede. Resources in JARs in the given JAR follow resources
     * directly in the given JARs.
     *
     * Note that URLClassLoader#findResources is public while ClassLoader#findResources is protected.
     */
    /*
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
    */

    /**
     * Defines a class with given class name from JARs embedded in the plugin JAR.
     *
     * It tries to continue even if Exceptions are throws in one of embedded JARs so that
     * it can find the target class without affected from other unrelated JARs.
     */
    /*
    private Class<?> defineClassFromContainedJars(final String className) throws ClassNotFoundException {
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
    */

    /*
    private void loadBytesToCache(
            final JarEntry entry,
            final InputStream is,
            final String jar,
            final Manifest manifest)
            throws IOException {
        final String entryName = entry.getName();
        final int indexLastPeriod = entryName.lastIndexOf('.');
        final String type = entryName.substring(indexLastPeriod + 1);

        final int indexLastSlash = entryName.lastIndexOf('/', indexLastPeriod - 1);
        if (entryName.endsWith(".class") && indexLastSlash > -1) {
            final String packageName = entryName.substring(0, indexLastSlash).replace('/', '.');
            if (this.getPackage(packageName) == null) {
                // Defend against null manifest.
                if (manifest != null) {
                    this.definePackageFromManifest(packageName, manifest, urlFactory.getCodeBase(jar));
                } else {
                    this.definePackage(packageName, null, null, null, null, null, null, null);
                }
            }
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(is, baos);

        if (type.equals("class")) {
            // If entry is a class, check to see that it hasn't been defined
            // already.  Class names must be unique within a classloader because
            // they are cached inside the VM until the classloader is released.
            if (this.isByteCodeCached(entryName, jar, baos)) {
                return;
            }
            this.cachedByteCode.put(entryName, new ByteCode(entryName, entry.getName(), baos, jar, manifest));
            // LOGGER.fine("cached bytes for class " + entryName);
        } else {
            // Another kind of resource.  Cache this by name, and also prefixed
            // by the jar name.  Don't duplicate the bytes.  This allows us
            // to map resource lookups to either jar-local, or globally defined.
            final String localName = jar + "/" + entryName;
            this.cachedByteCode.put(localName, new ByteCode(localName, entry.getName(), baos, jar, manifest));

            // Keep a set of jar names so we can do multiple-resource lookup by name
            // as in findResources().
            cachedJarNames.add(jar);
            // LOGGER.fine("cached bytes for local name " + localname);

            // Only keep the first non-local entry: this is like classpath where the first
            // to define wins.
            if (this.isByteCodeCached(entryName, jar, baos)) {
                return;
            }
            this.cachedByteCode.put(entryName, new ByteCode(entryName, entry.getName(), baos, jar, manifest));

            // LOGGER.fine("cached bytes for entry name " + entryName);
        }
    }
    */

    private final ClassLoader containerJarClassLoader;
    private final List<String> jarResourceNames;

    /** Maps in-JAR resource name to ResourceContent */
    private final Map<String, ResourceContent> resourceContents;

    /** Maps contained-JAR resource name to Manifest */
    private final Map<String, Manifest> manifests;

    private final Collection<Throwable> exceptions;
}
