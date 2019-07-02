package org.embulk.deps.timestamp;

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalInt;
import org.embulk.deps.EmbulkDependencyClassLoaders;

public abstract class TimestampFormatter {
    public static TimestampFormatter create(
            final String pattern,
            final Optional<ZoneOffset> defaultZoneOffset) {
        try {
            return CONSTRUCTOR.newInstance(
                    pattern,
                    defaultZoneOffset,
                    Optional.empty(),
                    OptionalInt.empty(),
                    OptionalInt.empty(),
                    OptionalInt.empty());
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
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

    public static TimestampFormatter createLegacy(
            final String pattern,
            final ZoneId defaultZoneId,
            final int defaultYear,
            final int defaultMonthOfYear,
            final int defaultDayOfMonth) {
        try {
            return CONSTRUCTOR.newInstance(
                    pattern,
                    Optional.empty(),
                    Optional.ofNullable(defaultZoneId),
                    OptionalInt.of(defaultYear),
                    OptionalInt.of(defaultMonthOfYear),
                    OptionalInt.of(defaultDayOfMonth));
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
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

    public static TimestampFormatter createLegacy(final String pattern, final String defaultZone, final String defaultDate) {
        try {
            return CONSTRUCTOR_STRING.newInstance(pattern, defaultZone, defaultDate);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
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

    public abstract Instant parse(String text);

    @SuppressWarnings("unchecked")
    private static Class<TimestampFormatter> loadImplClass() {
        try {
            return (Class<TimestampFormatter>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for timestamp are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.ofTimestamp();
    private static final String CLASS_NAME = "org.embulk.deps.timestamp.TimestampFormatterImpl";

    static {
        final Class<TimestampFormatter> clazz = loadImplClass();

        try {
            CONSTRUCTOR = clazz.getConstructor(
                    String.class, Optional.class, Optional.class, OptionalInt.class, OptionalInt.class, OptionalInt.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for timestamp are not loaded correctly: " + CLASS_NAME, ex);
        }

        try {
            CONSTRUCTOR_STRING = clazz.getConstructor(String.class, String.class, String.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for timestamp are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<TimestampFormatter> CONSTRUCTOR;
    private static final Constructor<TimestampFormatter> CONSTRUCTOR_STRING;
}
