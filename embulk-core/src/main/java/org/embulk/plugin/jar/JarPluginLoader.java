package org.embulk.plugin.jar;

import java.io.IOError;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.embulk.plugin.PluginClassLoader;
import org.embulk.plugin.PluginClassLoaderFactory;

/**
 * JarPluginLoader loads a JAR-based Embulk plugin class.
 *
 * It implements {@code AutoCloseable} so that it can load the contents lazily in the future.
 * It is recommended to try-with-resources to use this class.
 */
public class JarPluginLoader implements AutoCloseable {
    public static final String MANIFEST_PLUGIN_MAIN_CLASS = "Embulk-Plugin-Main-Class";
    public static final String MANIFEST_PLUGIN_SPI_VERSION = "Embulk-Plugin-Spi-Version";
    public static final String MANIFEST_PLUGIN_CLASS_PATH = "Embulk-Plugin-Class-Path";

    private JarPluginLoader(final Manifest pluginManifest,
                            final Attributes pluginManifestAttributes,
                            final Class pluginMainClass) {
        this.pluginManifest = pluginManifest;
        this.pluginManifestAttributes = pluginManifestAttributes;
        this.pluginMainClass = pluginMainClass;
    }

    public static JarPluginLoader load(
            final Path jarPath,
            final List<Path> dependencyJarPaths,
            final PluginClassLoaderFactory classLoaderFactory)
            throws InvalidJarPluginException {
        final JarURLConnection connection = openJarUrlConnection(jarPath);
        final Manifest manifest = loadJarPluginManifest(connection, jarPath);
        final Attributes manifestAttributes = manifest.getMainAttributes();
        final int spiVersion = getPluginSpiVersionFromManifest(manifestAttributes);

        if (spiVersion == 0) {
            final String mainClassName = getPluginMainClassNameFromManifest(manifestAttributes);
            final List<String> pluginClassPath = getPluginClassPathFromManifest(manifestAttributes);
            final Class mainClass = loadJarPluginMainClass(
                    jarPath, dependencyJarPaths, mainClassName, pluginClassPath, classLoaderFactory);
            return new JarPluginLoader(manifest, manifestAttributes, mainClass);
        }

        throw new InvalidJarPluginException("Unknown SPI version of JAR plugin: " + spiVersion);
    }

    public Class getPluginMainClass() throws InvalidJarPluginException {
        return this.pluginMainClass;
    }

    public int getPluginSpiVersion() throws InvalidJarPluginException {
        return getPluginSpiVersionFromManifest(this.pluginManifestAttributes);
    }

    @Override
    public void close() throws InvalidJarPluginException {}

    /**
     * Opens JarURLConnection for the given JAR file path.
     *
     * Note that JarURLConnection does not have {@code disconnect} nor {@code close}.
     */
    private static JarURLConnection openJarUrlConnection(final Path jarPath) throws InvalidJarPluginException {
        // jar:file:/...
        final URL jarUrl;
        try {
            jarUrl = new URL("jar:" + jarPath.toUri().toURL().toString() + "!/");
        } catch (MalformedURLException ex) {
            throw new InvalidJarPluginException("JAR plugin path specified is invalid: " + jarPath.toString(), ex);
        }

        try {
            return (JarURLConnection) jarUrl.openConnection();
        } catch (IOException ex) {
            throw new InvalidJarPluginException("JAR plugin specified is formatted wrongly: " + jarPath.toString(), ex);
        }
    }

    private static Manifest loadJarPluginManifest(final JarURLConnection connection, final Path jarPath)
            throws InvalidJarPluginException {
        try {
            return connection.getManifest();
        } catch (IOException ex) {
            throw new InvalidJarPluginException("Manifest in JAR plugin specified is invalid: " + jarPath.toString(),
                                                ex);
        }
    }

    private static Class loadJarPluginMainClass(final Path jarPath,
                                                final List<Path> dependencyJarPaths,
                                                final String pluginMainClassName,
                                                final List<String> pluginClassPath,
                                                final PluginClassLoaderFactory pluginClassLoaderFactory)
            throws InvalidJarPluginException {
        final URI fileUriJar;
        try {
            fileUriJar = jarPath.toUri();
        } catch (IOError ex) {
            throw new InvalidJarPluginException("[FATAL] JAR plugin path specified is invalid: " + jarPath.toString(),
                                                ex);
        } catch (SecurityException ex) {
            throw new InvalidJarPluginException("Security manager prohibits getting the working directory: "
                                                + jarPath.toString()
                                                + ". Specifying an absolute path for JAR plugin may solve this.",
                                                ex);
        }

        // file:/...
        final URL fileUrlJar;
        try {
            fileUrlJar = fileUriJar.toURL();
        } catch (IllegalArgumentException ex) {
            throw new InvalidJarPluginException("[FATAL/INTERNAL] JAR plugin path as URI is not absolute.", ex);
        } catch (MalformedURLException ex) {
            throw new InvalidJarPluginException("JAR plugin path specified is invalid: " + jarPath.toString(), ex);
        }

        final List<URL> dependencyJarUrls = new ArrayList<>();
        for (final Path dependencyJarPath : dependencyJarPaths) {
            final URI dependencyJarUri;
            try {
                dependencyJarUri = dependencyJarPath.toUri();
            } catch (IOError ex) {
                throw new InvalidJarPluginException(
                        "[FATAL] Path of dependency specified is invalid: " + dependencyJarPath.toString(), ex);
            } catch (SecurityException ex) {
                throw new InvalidJarPluginException("Security manager prohibits getting the working directory.", ex);
            }

            try {
                dependencyJarUrls.add(dependencyJarUri.toURL());
            } catch (IllegalArgumentException ex) {
                throw new InvalidJarPluginException("[FATAL/INTERNAL] Path of dependency is not absolute.", ex);
            } catch (MalformedURLException ex) {
                throw new InvalidJarPluginException(
                        "Path of dependency specified is invalid: " + dependencyJarUri.toString(), ex);
            }
        }

        final PluginClassLoader pluginClassLoader;
        if (!dependencyJarPaths.isEmpty()) {
            pluginClassLoader = pluginClassLoaderFactory.createForNestedJarWithDependencies(
                    JarPluginLoader.class.getClassLoader(),
                    fileUrlJar,
                    pluginClassPath,
                    dependencyJarUrls);
        } else if (pluginClassPath.isEmpty()) {
            pluginClassLoader = pluginClassLoaderFactory.createForNestedJar(
                    JarPluginLoader.class.getClassLoader(), fileUrlJar);
        } else {
            pluginClassLoader = pluginClassLoaderFactory.createForNestedJar(
                    JarPluginLoader.class.getClassLoader(), fileUrlJar, pluginClassPath);
        }

        final Class pluginMainClass;
        try {
            pluginMainClass = pluginClassLoader.loadClass(pluginMainClassName);
        } catch (ClassNotFoundException ex) {
            throw new InvalidJarPluginException("Class " + pluginMainClassName + " not found in " + jarPath.toString(),
                                                ex);
        }

        return pluginMainClass;
    }

    private static int getPluginSpiVersionFromManifest(final Attributes manifestAttributes)
            throws InvalidJarPluginException {
        final String spiVersionString = getAttributeFromManifest(manifestAttributes, MANIFEST_PLUGIN_SPI_VERSION);

        if (spiVersionString == null) {
            throw new InvalidJarPluginException("SPI version of JAR plugin is not specified.");
        }

        try {
            return Integer.parseInt(spiVersionString);
        } catch (NumberFormatException ex) {
            throw new InvalidJarPluginException("SPI version of JAR plugin is not an integer: \""
                                                + spiVersionString
                                                + "\"", ex);
        }
    }

    private static String getPluginMainClassNameFromManifest(final Attributes manifestAttributes)
            throws InvalidJarPluginException {
        final String pluginMainClassName = getAttributeFromManifest(manifestAttributes, MANIFEST_PLUGIN_MAIN_CLASS);

        if (pluginMainClassName == null) {
            throw new InvalidJarPluginException("Main class name of JAR plugin is not specified.");
        }

        return pluginMainClassName;
    }

    private static List<String> getPluginClassPathFromManifest(final Attributes manifestAttributes)
            throws InvalidJarPluginException {
        final String pluginClassPathJoined = getAttributeFromManifest(manifestAttributes, MANIFEST_PLUGIN_CLASS_PATH);

        if (pluginClassPathJoined == null) {
            return Collections.<String>emptyList();
        }

        final List<String> pluginClassPath = new ArrayList<String>();
        for (final String splitPluginClassPath : pluginClassPathJoined.split(" +", 0)) {
            pluginClassPath.add(splitPluginClassPath);
        }
        return pluginClassPath;
    }

    private static String getAttributeFromManifest(final Attributes manifestAttributes, final String attributeName)
            throws InvalidJarPluginException {
        try {
            return manifestAttributes.getValue(attributeName);
        } catch (IllegalArgumentException ex) {
            throw new InvalidJarPluginException("[FATAL/INTERNAL] "
                                                + attributeName
                                                + " is considered invalid as a manifest attribute.",
                                                ex);
        }
    }

    private final Manifest pluginManifest;
    private final Attributes pluginManifestAttributes;
    private final Class pluginMainClass;
}
