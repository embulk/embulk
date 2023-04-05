package org.embulk.plugin.maven;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.embulk.deps.EmbulkDependencyClassLoaders;

public abstract class ComparableVersion implements Comparable<ComparableVersion> {
    public static ComparableVersion of(final String version) {
        try {
            return CONSTRUCTOR.newInstance(version);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("Dependencies for Maven are not loaded correctly: " + CLASS_NAME, ex);
        } catch (final InvocationTargetException ex) {
            final Throwable targetException = ex.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error) {
                throw (Error) targetException;
            } else {
                throw new RuntimeException("Unexpected Exception in creating: " + CLASS_NAME, ex);
            }
        }
    }

    @Override
    public abstract int compareTo(final ComparableVersion other);

    @SuppressWarnings("unchecked")
    private static Class<ComparableVersion> loadImplClass() {
        try {
            return (Class<ComparableVersion>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Maven are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.get();
    private static final String CLASS_NAME = "org.embulk.deps.maven.ComparableVersionImpl";

    static {
        final Class<ComparableVersion> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor(String.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Maven are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<ComparableVersion> CONSTRUCTOR;
}
