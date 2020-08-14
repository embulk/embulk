package org.embulk.deps.timestamp;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.temporal.TemporalAccessor;
import org.embulk.deps.EmbulkDependencyClassLoaders;

public abstract class RubyDateTimeFormatter {
    public static RubyDateTimeFormatter create(final String pattern) {
        try {
            return CONSTRUCTOR.newInstance(pattern);
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

    public abstract String format(final TemporalAccessor temporal);

    @SuppressWarnings("unchecked")
    private static Class<RubyDateTimeFormatter> loadImplClass() {
        try {
            return (Class<RubyDateTimeFormatter>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Timestamp are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.get();
    private static final String CLASS_NAME = "org.embulk.deps.timestamp.RubyDateTimeFormatterImpl";

    static {
        final Class<RubyDateTimeFormatter> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor(String.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Timestamp are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<RubyDateTimeFormatter> CONSTRUCTOR;
}
