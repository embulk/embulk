package org.embulk.deps;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Loads classes of embulk-core's hidden dependencies.
 */
final class DependencyClassLoader extends SelfContainedJarAwareURLClassLoader {
    DependencyClassLoader(final Collection<Path> jarPaths, final ClassLoader parent) {
        // The delegation parent ClassLoader is processed by the super class URLClassLoader.
        super(toUrls(jarPaths), parent, EmbulkSelfContainedJarFiles.CORE);
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
}
