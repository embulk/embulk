package org.embulk.deps;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Loads classes and resources from self-contained JAR file resources, and a search path with {@link java.net.URLClassLoader}.
 *
 * <p>JARs files inside the Embulk JAR file are accessed only when the {@link java.net.URLClassLoader} implementation does
 * not find the requested resource. In other words, the delegation parent {@code ClassLoader} and a search path processed by
 * {@link java.net.URLClassLoader} are always prioritized over self-contained JAR files.
 *
 * @see <a href="https://www.ibm.com/developerworks/library/j-onejar/">Simplify your application delivery with One-JAR</a>
 */
final class DependencyClassLoader extends URLClassLoader {
    DependencyClassLoader(final Collection<Path> jarPaths, final ClassLoader parent) {
        // The delegation parent ClassLoader is processed by the super class URLClassLoader.
        super(toUrls(jarPaths), parent);
        this.accessControlContext = AccessController.getContext();
    }

    @Override
    protected void addURL(final URL url) {
        throw new UnsupportedOperationException("DependencyClassLoader does not support addURL.");
    }

    @Override
    public void close() throws IOException {
        super.close();
        // TODO: Close EmbulkSelfContainedJarFiles?
    }

    @Override
    public URL[] getURLs() {
        return super.getURLs();  // TODO: Add jar: URLs of self-contained JAR files.
    }

    /**
     * Finds the class with the specified binary name.
     *
     * <p>It should not be called when the class has already been loaded. The default {@code loadClass} checks
     * if the class has already been loaded before calling {@code findClass}.
     */
    @Override
    protected Class<?> findClass(final String className) throws ClassNotFoundException {
        try {
            // super.findClass(className) finds both from the delegation parent ClassLoader and non-self-contained JARs.
            return super.findClass(className);
        } catch (final ClassNotFoundException ignored) {
            // Pass through intentionally. Try finding from self-contained JARs.
        }

        // Try finding from self-contained JARs only when not found from the parent ClassLoader nor filesystem JARs.
        try {
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<Class<?>>() {
                    @Override
                    public Class<?> run() throws ClassNotFoundException {
                        try {
                            return defineClassFromEmbulkSelfContainedJarFiles(className);
                        } catch (final ClassNotFoundException | LinkageError | ClassCastException ex) {
                            throw ex;
                        } catch (final Throwable ex) {
                            // Found a resource in the container JAR, but failed to load it as a class.
                            throw new ClassNotFoundException(className, ex);
                        }
                    }
                },
                this.accessControlContext);
        } catch (final PrivilegedActionException ex) {
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

    // "protected String findLibrary(String name)" is not overridden because Embulk does not assume native libraries.

    // TODO: (maybe) Override "loadClass" so that it can deal with the context class loader.
    // Refer to {@link com.simontuffs.onejar.JarClassLoader#loadClass}.

    // TODO: (maybe) Override "getResource" so that it can delegate to external class loader explicitly.
    // Refer to {@link com.simontuffs.onejar.JarClassLoader#getResource}.

    /**
     * Finds a resource recognized as the given name.
     *
     * <p>Resources found by the delegation parent {@link java.net.URLClassLoader} are always prioritized. Resources in
     * self-contained JAR file resources are looked into only when not found by the delegation parent {@code URLClassLoader}.
     *
     * <p>Note that {@link java.net.URLClassLoader#findResource} is public while {@link java.lang.ClassLoader#findResource}
     * is protected.
     *
     * @param resourceName  name of target resource
     * @return URL of the resource
     */
    @Override
    public URL findResource(final String resourceName) {
        // super.findResource(resourceName) finds both from the delegation parent ClassLoader and non-self-contained JARs.
        final URL resourceUrlFromSuper = super.findResource(resourceName);
        if (resourceUrlFromSuper != null) {
            return resourceUrlFromSuper;
        }

        // TODO: Consider duplicated resources.
        final Resource resource = EmbulkSelfContainedJarFiles.getSingleResource(resourceName);
        if (resource == null) {
            return null;
        }

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                    @Override
                    public URL run() throws MalformedURLException {
                        return resource.buildJarEmbeddedUrl();
                    }
                }, this.accessControlContext);
        } catch (final PrivilegedActionException ignored) {
            // Pass through intentionally.
        }
        return null;
    }

    /**
     * Finds resources recognized as the given name.
     *
     * <p>Note that {@link java.net.URLClassLoader#findResources} is public while {@link java.lang.ClassLoader#findResources}
     * is protected.
     */
    @Override
    public Enumeration<URL> findResources(final String resourceName) throws IOException {
        // super.findResources(resourceName) finds both from the delegation parent ClassLoader and non-self-contained JARs.
        final Enumeration<URL> resourceUrlsFromSuper = super.findResources(resourceName);

        // Even if some resources are found from the delegation parent class loader, it looks into self-contained JAR files.
        final Collection<Resource> resources = EmbulkSelfContainedJarFiles.getMultipleResources(resourceName);

        final Vector<URL> resourceUrls = new Vector<URL>();
        while (resourceUrlsFromSuper.hasMoreElements()) {
            resourceUrls.add(resourceUrlsFromSuper.nextElement());
        }
        final Collection<URL> resourceUrlsFromSelfContainedJarFiles;
        try {
            resourceUrlsFromSelfContainedJarFiles =
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Collection<URL>>() {
                            @Override
                            public Collection<URL> run() throws IOException {
                                final ArrayList<URL> urls = new ArrayList<>();
                                for (final Resource resource : resources) {
                                    urls.add(resource.buildJarEmbeddedUrl());
                                }
                                return urls;
                            }
                        },
                        accessControlContext);
        } catch (final PrivilegedActionException ignored) {
            // Passing through intentionally.
            return resourceUrls.elements();
        }

        resourceUrls.addAll(resourceUrlsFromSelfContainedJarFiles);
        return resourceUrls.elements();
    }

    private Class<?> defineClassFromEmbulkSelfContainedJarFiles(final String className) throws ClassNotFoundException {
        final String resourceName = className.replace('.', '/').concat(".class");

        // Class must be singular.
        final Resource resource = EmbulkSelfContainedJarFiles.getSingleResource(resourceName);
        if (resource == null) {
            throw new ClassNotFoundException(className);
        }
        final URL codeSourceUrl = resource.getCodeSourceUrl();

        final int indexLastPeriod = className.lastIndexOf('.');
        if (indexLastPeriod != -1) {  // If |className| has a package part.
            final String packageName = className.substring(0, indexLastPeriod);
            final Manifest manifest = resource.getManifest();  // Class must be singular.

            if (!this.checkPackageSealing(packageName, manifest, codeSourceUrl)) {
                try {
                    if (manifest != null) {
                        this.definePackageFromManifest(packageName, manifest, codeSourceUrl);
                    } else {
                        this.definePackage(packageName, null, null, null, null, null, null, null);
                    }
                } catch (final IllegalArgumentException ex) {
                    if (!this.checkPackageSealing(packageName, manifest, codeSourceUrl)) {
                        throw new ClassNotFoundException(
                                "FATAL: Unexpected double failures to define package: " + packageName, ex);
                    }
                }
            }
        }

        final CodeSource codeSource = new CodeSource(codeSourceUrl, resource.getCodeSigners());
        return this.defineClass(className, resource.getAdjustedByteBuffer(), codeSource);
    }

    private boolean checkPackageSealing(final String packageName, final Manifest manifest, final URL url) {
        final Package packageInstance = this.getPackage(packageName);

        if (packageInstance == null) {
            return false;
        }

        if (packageInstance.isSealed()) {
            if (!packageInstance.isSealed(url)) {
                throw new SecurityException(String.format(
                        "Package \"%s\" is already loaded, and sealed with a different code source URL.", packageName));
            }
        } else {
            if ((manifest != null) && isManifestToSeal(packageName, manifest)) {
                throw new SecurityException(String.format(
                        "Package \"%s\" is already loaded, and unsealed.", packageName));
            }
        }
        return true;
    }

    private static boolean isManifestToSeal(final String packageName, final Manifest manifest) {
        final Optional<Attributes> perEntryAttributes =
                Optional.ofNullable(manifest.getAttributes(packageName.replace('.', '/').concat("/")));
        final Attributes mainAttributes = manifest.getMainAttributes();

        return "true".equalsIgnoreCase(
                getEffectiveAttribute(mainAttributes, perEntryAttributes, Attributes.Name.SEALED));
    }

    private Package definePackageFromManifest(final String packageName, final Manifest manifest, final URL codeSourceUrl)
            throws IllegalArgumentException {
        // https://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Per-Entry_Attributes
        final Optional<Attributes> perEntryAttributes =
                Optional.ofNullable(manifest.getAttributes(packageName.replace('.', '/').concat("/")));
        final Attributes mainAttributes = manifest.getMainAttributes();

        return this.definePackage(
                packageName,
                getEffectiveAttribute(mainAttributes, perEntryAttributes, Attributes.Name.SPECIFICATION_TITLE),
                getEffectiveAttribute(mainAttributes, perEntryAttributes, Attributes.Name.SPECIFICATION_VERSION),
                getEffectiveAttribute(mainAttributes, perEntryAttributes, Attributes.Name.SPECIFICATION_VENDOR),
                getEffectiveAttribute(mainAttributes, perEntryAttributes, Attributes.Name.IMPLEMENTATION_TITLE),
                getEffectiveAttribute(mainAttributes, perEntryAttributes, Attributes.Name.IMPLEMENTATION_VERSION),
                getEffectiveAttribute(mainAttributes, perEntryAttributes, Attributes.Name.IMPLEMENTATION_VENDOR),
                "true".equalsIgnoreCase(
                        getEffectiveAttribute(mainAttributes, perEntryAttributes, Attributes.Name.SEALED))
                        ? codeSourceUrl
                        : null);
    }

    private static String getEffectiveAttribute(
            final Attributes mainAttributes,
            final Optional<Attributes> perEntryAttributes,
            final Attributes.Name attributeName) {
        final String mainAttribute = mainAttributes.getValue(attributeName);
        return (String) perEntryAttributes.orElse(mainAttributes).getOrDefault(attributeName, mainAttribute);
    }

    private static URL[] toUrls(final Collection<Path> jarPaths) {
        final URL[] jarUrls = new URL[jarPaths.size()];

        int index = 0;
        for (final Path jarPath : jarPaths) {
            try {
                jarUrls[index] = jarPath.toUri().toURL();
            } catch (final MalformedURLException ex) {
                throw new LinkageError("Invalid path to JAR: " + jarPath.toString(), ex);
            }
            ++index;
        }
        return jarUrls;
    }

    private final AccessControlContext accessControlContext;
}
