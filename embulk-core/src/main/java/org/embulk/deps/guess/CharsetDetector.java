package org.embulk.deps.guess;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.embulk.deps.DependencyCategory;
import org.embulk.deps.EmbulkDependencyClassLoaders;

public abstract class CharsetDetector {
    public static CharsetDetector create() {
        try {
            return CONSTRUCTOR.newInstance();
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("Dependencies for Guess are not loaded correctly: " + CLASS_NAME, ex);
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

    public abstract CharsetDetector setText(final byte[] in);

    public abstract CharsetMatch detect();

    @SuppressWarnings("unchecked")
    private static Class<CharsetDetector> loadImplClass() {
        try {
            return (Class<CharsetDetector>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Guess are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.of(DependencyCategory.GUESS);
    private static final String CLASS_NAME = "org.embulk.deps.guess.CharsetDetectorImpl";

    static {
        final Class<CharsetDetector> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor();
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Guess are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<CharsetDetector> CONSTRUCTOR;
}
