package org.embulk.deps;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

class LocalFileDependencyClassLoader extends DependencyClassLoader {
    private LocalFileDependencyClassLoader(final URLClassLoader urlClassLoader) {
        super(urlClassLoader);
    }

    static LocalFileDependencyClassLoader ofUrl(
            final ClassLoader parentClassLoader,
            final Collection<URL> localJarUrls) {
        return new LocalFileDependencyClassLoader(new URLClassLoader(
                localJarUrls.toArray(new URL[localJarUrls.size()]),
                parentClassLoader));
    }

    static LocalFileDependencyClassLoader of(
            final ClassLoader parentClassLoader,
            final Collection<Path> localJarPaths) {
        final ArrayList<URL> localJarUrls = new ArrayList<>();
        for (final Path localJarPath : localJarPaths) {
            final URL url;
            try {
                url = localJarPath.toUri().toURL();
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
            localJarUrls.add(url);
        }
        return ofUrl(parentClassLoader, localJarUrls);
    }
}
