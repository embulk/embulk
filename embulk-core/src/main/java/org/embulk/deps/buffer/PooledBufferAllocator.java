package org.embulk.deps.buffer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.embulk.deps.DependencyCategory;
import org.embulk.deps.EmbulkDependencyClassLoaders;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferAllocator;

public abstract class PooledBufferAllocator implements BufferAllocator {
    public static PooledBufferAllocator create(final int pageSize) {
        try {
            return CONSTRUCTOR.newInstance(pageSize);
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

    public static PooledBufferAllocator create() {
        return create(DEFAULT_PAGE_SIZE);
    }

    @Override
    public abstract Buffer allocate();

    @Override
    public abstract Buffer allocate(final int minimumCapacity);

    @SuppressWarnings("unchecked")
    private static Class<PooledBufferAllocator> loadImplClass() {
        try {
            return (Class<PooledBufferAllocator>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Buffer are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final int DEFAULT_PAGE_SIZE = 32 * 1024;

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.of(DependencyCategory.BUFFER);
    private static final String CLASS_NAME = "org.embulk.deps.buffer.PooledBufferAllocatorImpl";

    static {
        final Class<PooledBufferAllocator> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor(int.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Buffer are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<PooledBufferAllocator> CONSTRUCTOR;
}
