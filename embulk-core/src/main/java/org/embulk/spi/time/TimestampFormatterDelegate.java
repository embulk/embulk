package org.embulk.spi.time;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import org.embulk.deps.EmbulkDependencyClassLoaders;

public abstract class TimestampFormatterDelegate {
    public static TimestampFormatterDelegate of(final String pattern, final String defaultZone, final String defaultDate) {
        try {
            return CONSTRUCTOR.newInstance(pattern, defaultZone, defaultDate);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("Dependencies for Timestamp are not loaded correctly: " + CLASS_NAME, ex);
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

    public static TimestampFormatterDelegate of(final String pattern, final String defaultZone) {
        return of(pattern, defaultZone, null);
    }

    public static TimestampFormatterDelegate of(final String pattern) {
        return of(pattern, null, null);
    }

    public abstract String format(Instant format);

    public abstract Instant parse(String text);

    @SuppressWarnings("unchecked")
    private static Class<TimestampFormatterDelegate> loadImplClass() {
        try {
            return (Class<TimestampFormatterDelegate>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Timestamp are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.get();
    private static final String CLASS_NAME = "org.embulk.deps.timestamp.TimestampFormatterDelegateImpl";

    static {
        final Class<TimestampFormatterDelegate> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor(String.class, String.class, String.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Timestamp are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<TimestampFormatterDelegate> CONSTRUCTOR;
}
