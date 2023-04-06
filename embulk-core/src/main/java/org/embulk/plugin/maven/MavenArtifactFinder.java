package org.embulk.plugin.maven;

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import org.embulk.EmbulkDependencyClassLoader;
import org.embulk.plugin.MavenPluginType;

public abstract class MavenArtifactFinder {
    public static MavenArtifactFinder create(final Path localMavenRepositoryPath) throws FileNotFoundException {
        try {
            return CONSTRUCTOR.newInstance(localMavenRepositoryPath);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("Dependencies for Maven are not loaded correctly: " + CLASS_NAME, ex);
        } catch (final InvocationTargetException ex) {
            final Throwable targetException = ex.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error) {
                throw (Error) targetException;
            } else if (targetException instanceof FileNotFoundException) {
                throw (FileNotFoundException) targetException;
            } else {
                throw new RuntimeException("Unexpected Exception in creating: " + CLASS_NAME, ex);
            }
        }
    }

    /**
     * Finds a Maven-based plugin JAR with its "direct" dependencies.
     *
     * @see <a href="https://github.com/eclipse/aether-demo/blob/322fa556494335faaf3ad3b7dbe8f89aaaf6222d/aether-demo-snippets/src/main/java/org/eclipse/aether/examples/GetDirectDependencies.java">aether-demo's GetDirectDependencies.java</a>
     */
    public abstract MavenPluginPaths findMavenPluginJarsWithDirectDependencies(
            final MavenPluginType pluginType,
            final String category)
            throws FileNotFoundException;

    @SuppressWarnings("unchecked")
    private static Class<MavenArtifactFinder> loadImplClass() {
        try {
            return (Class<MavenArtifactFinder>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Maven are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoader.get();
    private static final String CLASS_NAME = "org.embulk.deps.maven.MavenArtifactFinderImpl";

    static {
        final Class<MavenArtifactFinder> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor(Path.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Maven are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<MavenArtifactFinder> CONSTRUCTOR;
}
