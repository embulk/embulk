package org.embulk.deps.airlift;

import java.lang.reflect.Constructor;

/**
 * Proxies callings to io.airlift.slice.Slices.
 *
 * <p>It works like a Singleton, just one instance in the entire Java runtime, not per Embulk's execution session.
 * Singleton is fine for this because use of the library should not vary per execution session.
 */
public abstract class Slices {
    public abstract org.embulk.deps.airlift.Slice wrappedBuffer(final byte[] array, final int offset, final int length);

    public static synchronized void setClassLoader(final ClassLoader classLoaderNew) {
        if (classLoaderNew == null) {
            throw new IllegalArgumentException("ClassLoader must not be null.");
        }
        if (classLoader != null) {
            throw new RuntimeException("ClassLoader is set twice.");
        }
        classLoader = classLoaderNew;
    }

    public static Slices get() {
        return InitializeOnDemandHolder.impl;
    }

    private static class InitializeOnDemandHolder {
        public static final Class<Slices> clazz;
        public static final Slices impl;

        static {
            try {
                @SuppressWarnings("unchecked")
                final Class<Slices> c = (Class<Slices>) (classLoader.loadClass("org.embulk.deps.airlift.SlicesImpl"));
                clazz = c;
            } catch (final ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }

            final Constructor<Slices> constructor;
            try {
                constructor = clazz.getConstructor(CONSTRUCTOR_TYPES);
            } catch (final SecurityException | NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }

            try {
                impl = constructor.newInstance();
            } catch (final IllegalArgumentException | ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static final Class<?>[] CONSTRUCTOR_TYPES = {};

    private static ClassLoader classLoader = null;
}
