package org.embulk.jruby;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Loads classes and resources of JRuby artifact(s).
 *
 * <p>It prioritizes a class found by this class loader over a class loaded by the parent class loader because
 * this class loader is specific for JRuby. {@code jruby-complete-9.X.Y.Z.jar} contains all libraries required
 * in JRuby. Some of them conflict with {@code embulk-core}'s top-level dependencies, such as {@code "asm:asm"}
 * and {@code "org.sonatype.sisu.inject:cglib"} used from Guice.
 *
 * <p>JRuby should use JRuby's own dependencies, and Embulk should not be impacted from JRuby's dependencies.
 */
final class JRubyClassLoader extends URLClassLoader {
    JRubyClassLoader(final Collection<URL> jarUrls, final ClassLoader parent) {
        // The delegation parent ClassLoader is processed by the super class URLClassLoader.
        super(jarUrls.toArray(new URL[0]), parent);
    }

    @Override
    protected void addURL(final URL url) {
        throw new UnsupportedOperationException("JRubyClassLoader does not support addURL.");
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public URL[] getURLs() {
        return super.getURLs();
    }

    /**
     * Loads the class with the specified binary name from JRuby.
     *
     * <p>It prioritizes a class found by this class loader over a class loaded by the parent class loader.
     */
    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        synchronized (this.getClassLoadingLock(name)) {
            // If a class of the specified name has already been loaded by this class loader, or the parent class loader,
            // find the loaded class, and return it.
            final Class<?> loadedClass = this.findLoadedClass(name);
            if (loadedClass != null) {
                return this.resolveClassIfNeeded(loadedClass, resolve);
            }

            // JRuby should use Joda-Time of embulk-core (on the top-level class loader), not of jruby-complete.
            // Otherwise, embulk-core uses its own, and JRuby uses its own, then they wouldn't match.
            //
            // TODO: Remove the condition when embulk-core removes Joda-Time from its dependencies.
            if (!name.startsWith("org.joda.time.")) {
                // If a class of the specified name has not been loaded yet, and is found by this (not parent) class loader,
                // find it, and return it.
                try {
                    return this.resolveClassIfNeeded(this.findClass(name), resolve);
                } catch (final ClassNotFoundException ignored) {
                    // Passing through intentionally.
                }
            }

            // If a class of the specified name is found by this class loader (not by the parent class loader),
            // find it, and return it.
            try {
                return this.resolveClassIfNeeded(this.getParent().loadClass(name), resolve);
            } catch (final ClassNotFoundException ignored) {
                // Passing through intentionally.
            }

            throw new ClassNotFoundException(name);
        }
    }

    /**
     * Finds the resource with the given name from JRuby.
     *
     * <p>It prioritizes a resource found by this class loader over a class loaded by the parent class loader.
     */
    @Override
    public URL getResource(final String name) {
        final URL parentUrl = this.getParent().getResource(name);
        if (parentUrl != null) {
            return parentUrl;
        }

        return this.findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        final Vector<URL> resources = new Vector<>();

        final Enumeration<URL> parentResources = this.getParent().getResources(name);
        while (parentResources.hasMoreElements()) {
            resources.add(parentResources.nextElement());
        }

        final Enumeration<URL> childResources = this.findResources(name);
        while (childResources.hasMoreElements()) {
            resources.add(childResources.nextElement());
        }

        return resources.elements();
    }

    private Class<?> resolveClassIfNeeded(final Class<?> clazz, final boolean resolve) {
        if (resolve) {
            this.resolveClass(clazz);
        }
        return clazz;
    }
}
