package org.embulk.deps.timestamp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.embulk.deps.EmbulkDependencyClassLoaders;

public final class RubyDateTimeZones {
    private RubyDateTimeZones() {
        // No instantiation.
    }

    public static int toOffsetInSeconds(final String zoneName) {
        try {
            return (int) TO_OFFSET_IN_SECONDS.invoke(null, zoneName);
        } catch (final IllegalAccessException | IllegalArgumentException ex) {
            throw new LinkageError("Dependencies for timestamp are not loaded correctly: " + CLASS_NAME, ex);
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

    @SuppressWarnings("unchecked")
    private static Class<RubyDateTimeZones> loadImplClass() {
        try {
            return (Class<RubyDateTimeZones>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for timestamp are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.ofTimestamp();
    private static final String CLASS_NAME = "org.embulk.deps.timestamp.RubyDateTimeZonesImpl";

    static {
        final Class<RubyDateTimeZones> clazz = loadImplClass();

        try {
            TO_OFFSET_IN_SECONDS = clazz.getMethod("toOffsetInSeconds", String.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for timestamp are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Method TO_OFFSET_IN_SECONDS;
}
