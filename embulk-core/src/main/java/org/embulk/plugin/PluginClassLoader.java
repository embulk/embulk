package org.embulk.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import org.embulk.cli.SelfContainedJarAwareURLClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginClassLoader extends SelfContainedJarAwareURLClassLoader {
    private PluginClassLoader(
            final ClassLoader parentClassLoader,
            final Collection<URL> jarUrls,
            final String selfContainedPluginName) {
        super(jarUrls.toArray(new URL[0]), parentClassLoader, selfContainedPluginName);

        this.hasJep320LoggedWithStackTrace = false;
    }

    /**
     * Creates PluginClassLoader for plugins with dependency JARs.
     *
     * @param parentClassLoader  the parent ClassLoader of this PluginClassLoader instance
     * @param jarUrls  collection of "file:" URLs of all JARs related to the plugin
     * @return {@code PluginClassLoader} instance created
     */
    public static PluginClassLoader create(
            final ClassLoader parentClassLoader,
            final Collection<URL> jarUrls) {
        return new PluginClassLoader(
                parentClassLoader,
                jarUrls,
                null);
    }

    public static PluginClassLoader forSelfContainedPlugin(
            final ClassLoader parentClassLoader,
            final String selfContainedPluginName) {
        return new PluginClassLoader(
                parentClassLoader,
                new ArrayList<>(),
                selfContainedPluginName);
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
                    // If a class that is removed by JEP 320 is found here, it should be fine.
                    // It means that the class is found on the plugin side -- the plugin contains its own one.
                    // Classes removed by JEP 320 are not in the "parent-first" list.
                    return resolveClass(findClass(name), resolve);
                } catch (ClassNotFoundException ignored) {
                    // Passing through intentionally.
                }
            }

            // If the class is "parent-first" (to be loaded by the parent at first), try this part at first.
            // If the class is "not parent-first" (not to be loaded by the parent at first), the above part runs first.
            try {
                final Class<?> resolvedClass = resolveClass(getParent().loadClass(name), resolve);
                logInfoIfJep320Class(name);
                return resolvedClass;
            } catch (final ClassNotFoundException ex) {
                if (!parentFirst) {
                    rethrowIfJep320Class(name, ex);
                }
                // Passing through intentionally if the class is not not removed by JEP 320.
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
        final Vector<URL> resources = new Vector<>();

        boolean parentFirst = isParentFirstPath(name);

        if (!parentFirst) {
            final Enumeration<URL> childResources = findResources(name);
            while (childResources.hasMoreElements()) {
                resources.add(childResources.nextElement());
            }
        }

        final Enumeration<URL> parentResources = getParent().getResources(name);
        while (parentResources.hasMoreElements()) {
            resources.add(parentResources.nextElement());
        }

        if (parentFirst) {
            final Enumeration<URL> childResources = findResources(name);
            while (childResources.hasMoreElements()) {
                resources.add(childResources.nextElement());
            }
        }

        return resources.elements();
    }

    static boolean isParentFirstPackage(final String name) {
        final int dotIndex = name.lastIndexOf(".");

        final String packageNameWithLastDot;
        if (dotIndex < 0) {
            packageNameWithLastDot = "";
        } else {
            packageNameWithLastDot = name.substring(0, dotIndex + 1);
        }

        if (packageNameWithLastDot.startsWith("ch.qos.logback.classic.")) {
            return true;
        }
        if (packageNameWithLastDot.startsWith("ch.qos.logback.core.")) {
            return true;
        }
        if (packageNameWithLastDot.startsWith("java.")) {
            return true;
        }
        if (packageNameWithLastDot.startsWith("org.embulk.")) {
            return true;
        }
        if (packageNameWithLastDot.startsWith("org.msgpack.core.")) {
            return true;
        }
        if (packageNameWithLastDot.startsWith("org.msgpack.value.")) {
            return true;
        }
        if (packageNameWithLastDot.startsWith("org.slf4j.")) {
            return true;
        }

        return false;
    }

    static boolean isParentFirstPath(final String name) {
        if (name.startsWith("ch/qos/logback/classic/boolex/")) {
            return true;
        }
        if (name.startsWith("ch/qos/logback/classic/db/script/")) {
            return true;
        }
        if (name.startsWith("embulk/")) {
            return true;
        }
        if (name.startsWith("msgpack/")) {
            return true;
        }
        if (name.startsWith("org/embulk/")) {
            return true;
        }

        return false;
    }

    private synchronized void logInfoIfJep320Class(final String className) {
        final int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex != -1) {  // Found
            final String packageName = className.substring(0, lastDotIndex);
            if (JEP_320_PACKAGES.contains(packageName)) {
                if (!this.hasJep320LoggedWithStackTrace) {
                    // Logging with details and stack trace only against the first one.
                    // When a class is loaded from a library, more classes are usually loaded.
                    // It would be too noisy if details and stack trace are dumped every time.
                    logger.info(
                            "Class " + className + " is loaded by the parent ClassLoader, which is removed by JEP 320. "
                            + "The plugin needs to include it on the plugin side. "
                            + "See https://github.com/embulk/embulk/issues/1270 for more details.", new DummyStackTraceDump());
                    this.hasJep320LoggedWithStackTrace = true;
                } else {
                    logger.info("Class " + className + " is loaded by the parent ClassLoader, which is removed by JEP 320.");
                }
            }
        }
    }

    private void rethrowIfJep320Class(final String className, final ClassNotFoundException ex) throws ClassNotFoundException {
        final int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex != -1) {  // Found
            final String packageName = className.substring(0, lastDotIndex);
            if (JEP_320_PACKAGES.contains(packageName)) {
                throw new ClassNotFoundException(
                        "A plugin tried to load " + className + " with the parent ClassLoader. "
                        + "It is removed from JDK by JEP 320. The plugin needs to include it on the plugin side. "
                        + "See https://github.com/embulk/embulk/issues/1270 for more details.",
                        ex);
            }
        }
    }

    private static class DummyStackTraceDump extends Exception {
        DummyStackTraceDump() {
            super("[DUMMY] It is not an Exception. Just showing where classes which will be removed in JEP 320 are loaded.");
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(PluginClassLoader.class);

    /**
     * Packages that are deprecated and removed since Java 11 by JEP 320.
     *
     * @see <a href="https://openjdk.java.net/jeps/320#Description">JEP 320</a>
     */
    private static String[] JEP_320_PACKAGES_ARRAY = {
        // Module java.xml.ws : JAX-WS, plus the related technologies SAAJ and Web Services Metadata
        // https://docs.oracle.com/javase/9/docs/api/java.xml.ws-summary.html
        "javax.jws",
        "javax.jws.soap",
        "javax.xml.soap",
        "javax.xml.ws",
        "javax.xml.ws.handler",
        "javax.xml.ws.handler.soap",
        "javax.xml.ws.http",
        "javax.xml.ws.soap",
        "javax.xml.ws.spi",
        "javax.xml.ws.spi.http",
        "javax.xml.ws.wsaddressing",

        // Module java.xml.bind : JAXB
        // https://docs.oracle.com/javase/9/docs/api/java.xml.bind-summary.html
        "javax.xml.bind",
        "javax.xml.bind.annotation",
        "javax.xml.bind.annotation.adapters",
        "javax.xml.bind.attachment",
        "javax.xml.bind.helpers",
        "javax.xml.bind.util",

        // Module java.activation : JAF
        // https://docs.oracle.com/javase/9/docs/api/java.activation-summary.html
        "javax.activation",

        // Module java.xml.ws.annotation : Common Annotations
        // https://docs.oracle.com/javase/9/docs/api/java.xml.ws.annotation-summary.html
        "javax.annotation",

        // Module java.corba : CORBA
        // https://docs.oracle.com/javase/9/docs/api/java.corba-summary.html
        "javax.activity",
        "javax.rmi",
        "javax.rmi.CORBA",
        "org.omg.CORBA",
        "org.omg.CORBA_2_3",
        "org.omg.CORBA_2_3.portable",
        "org.omg.CORBA.DynAnyPackage",
        "org.omg.CORBA.ORBPackage",
        "org.omg.CORBA.portable",
        "org.omg.CORBA.TypeCodePackage",
        "org.omg.CosNaming",
        "org.omg.CosNaming.NamingContextExtPackage",
        "org.omg.CosNaming.NamingContextPackage",
        "org.omg.Dynamic",
        "org.omg.DynamicAny",
        "org.omg.DynamicAny.DynAnyFactoryPackage",
        "org.omg.DynamicAny.DynAnyPackage",
        "org.omg.IOP",
        "org.omg.IOP.CodecFactoryPackage",
        "org.omg.IOP.CodecPackage",
        "org.omg.Messaging",
        "org.omg.PortableInterceptor",
        "org.omg.PortableInterceptor.ORBInitInfoPackage",
        "org.omg.PortableServer",
        "org.omg.PortableServer.CurrentPackage",
        "org.omg.PortableServer.POAManagerPackage",
        "org.omg.PortableServer.POAPackage",
        "org.omg.PortableServer.portable",
        "org.omg.PortableServer.ServantLocatorPackage",
        "org.omg.SendingContext",
        "org.omg.stub.java.rmi",

        // Module java.transaction : JTA
        // https://docs.oracle.com/javase/9/docs/api/java.transaction-summary.html
        "javax.transaction",
    };

    private static Set<String> JEP_320_PACKAGES =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(JEP_320_PACKAGES_ARRAY)));

    private boolean hasJep320LoggedWithStackTrace;
}
