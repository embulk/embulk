package org.embulk.deps.buffer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.embulk.deps.DependencyCategory;
import org.embulk.deps.EmbulkDependencyClassLoaders;
import org.embulk.spi.Buffer;

public abstract class Slice {
    @SuppressWarnings("deprecation")  // Calling Buffer#array().
    public static Slice createWithWrappedBuffer(final Buffer buffer) {
        try {
            return CONSTRUCTOR.newInstance(buffer.array(), buffer.offset(), buffer.capacity());
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("Dependencies for Buffer are not loaded correctly: " + CLASS_NAME, ex);
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

    public abstract byte getByte(int index);

    public abstract void getBytes(int index, byte[] destination, int destinationIndex, int length);

    public abstract double getDouble(int index);

    public abstract int getInt(int index);

    public abstract long getLong(int index);

    public abstract void setByte(int index, int value);

    public abstract void setBytes(int index, byte[] source);

    public abstract void setDouble(int index, double value);

    public abstract void setInt(int index, int value);

    public abstract void setLong(int index, long value);

    @SuppressWarnings("unchecked")
    private static Class<Slice> loadImplClass() {
        try {
            return (Class<Slice>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Buffer are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.of(DependencyCategory.BUFFER);
    private static final String CLASS_NAME = "org.embulk.deps.buffer.SliceImpl";

    static {
        final Class<Slice> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor(byte[].class, int.class, int.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Buffer are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<Slice> CONSTRUCTOR;
}
