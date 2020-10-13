/*
 * Copyright 2014 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * A buffer used in and between plugins. It wraps a {@code byte} array.
 *
 * <p>It will be a pure {@code abstract class}, but it now has some {@code static} creator methods implemented.
 *
 * <p>Those {@code static} creator methods, {@link #allocate(int)}, {@link #copyOf(byte[])},
 * {@link #copyOf(byte[], int, int)}, {@link #wrap(byte[])}, and {@link #wrap(byte[], int, int)}
 * are implemented for compatibility for some legacy plugins calling them. They will be removed
 * before Embulk v1. Plugins should no longer call them, even in tests.
 *
 * <p>At the same time, a constant {@code Buffer.EMPTY} has already been removed. Plugins cannot use it anymore.
 *
 * @since 0.4.0
 */
public abstract class Buffer {
    protected Buffer() {
    }

    /**
     * Returns the internal {@code byte} array of this {@link Buffer}.
     *
     * @deprecated Accessing the internal {@code byte} array is not safe.
     * @return the internal {@code byte} array
     *
     * @since 0.4.0
     */
    @Deprecated  // Not for removal, but deprecated.
    public abstract byte[] array();

    /**
     * Returns the current offset of this {@link Buffer}.
     *
     * @return the current offset
     *
     * @since 0.4.0
     */
    public abstract int offset();

    /**
     * Sets the current offset of this {@link Buffer}.
     *
     * @param offset  the offset to set
     * @return this {@link Buffer} itself
     *
     * @since 0.4.0
     */
    public abstract Buffer offset(int offset);

    /**
     * Returns the current limit of this {@link Buffer}.
     *
     * @return the current limit
     *
     * @since 0.4.0
     */
    public abstract int limit();

    /**
     * Sets the current limit of this {@link Buffer}.
     *
     * @param limit  the limit to set
     * @return this {@link Buffer} itself
     *
     * @since 0.4.0
     */
    public abstract Buffer limit(int limit);

    /**
     * Returns the capacity of this {@link Buffer}.
     *
     * <p>The capacity does not change once created.
     *
     * @return the capacity
     *
     * @since 0.4.0
     */
    public abstract int capacity();

    /**
     * Copies an array from {@code source}, beginning at {@code sourceIndex}, to {@code index} of this {@link Buffer}.
     *
     * @param index  starting position in this destination {@link Buffer}
     * @param source  the source array
     * @param sourceIndex  starting position in the source array
     * @param length  the number of bytes to be copied
     *
     * @since 0.4.0
     */
    public abstract void setBytes(int index, byte[] source, int sourceIndex, int length);

    /**
     * Copies an array from {@code source}, beginning at {@code sourceIndex}, to {@code index} of this {@link Buffer}.
     *
     * @param index  starting position in this destination {@link Buffer}
     * @param source  the source {@link Buffer}
     * @param sourceIndex  starting position in the source {@link Buffer}
     * @param length  the number of bytes to be copied
     *
     * @since 0.4.0
     */
    public abstract void setBytes(int index, Buffer source, int sourceIndex, int length);

    /**
     * Copies an array from this {@link Buffer}, beginning at {@code index}, to {@code destIndex} of {@code dest}.
     *
     * @param index  starting position in this source {@link Buffer}
     * @param dest  the destination array
     * @param destIndex  starting position in the destination array
     * @param length  the number of bytes to be copied
     *
     * @since 0.4.0
     */
    public abstract void getBytes(int index, byte[] dest, int destIndex, int length);

    /**
     * Copies an array from this {@link Buffer}, beginning at {@code index}, to {@code destIndex} of {@code dest}.
     *
     * @param index  starting position in this source {@link Buffer}
     * @param dest  the destination {@link Buffer}
     * @param destIndex  starting position in the destination {@link Buffer}
     * @param length  the number of bytes to be copied
     *
     * @since 0.4.0
     */
    public abstract void getBytes(int index, Buffer dest, int destIndex, int length);

    /**
     * Releases this {@link Buffer}.
     *
     * @since 0.4.0
     */
    public abstract void release();

    /**
     * Creates a new {@link Buffer} instance.
     *
     * @deprecated  This method is to be removed. Plugins should no longer call it directly.
     * @param length  length of the created {@link Buffer}
     * @return created {@link Buffer}
     *
     * @since 0.4.0
     */
    @Deprecated
    public static Buffer allocate(final int length) {
        try {
            return Holder.CONSTRUCTOR.newInstance(new byte[length], 0, length);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("[FATAL] org.embulk.spi.BufferImpl is invalid.", ex);
        } catch (final InvocationTargetException ex) {
            throwCheckedForcibly(ex.getTargetException());
            return null;  // Should never reach.
        }
    }

    /**
     * Creates a new {@link Buffer} instance copied from {@code src}.
     *
     * @deprecated  This method is to be removed. Plugins should no longer call it directly.
     * @param src  the source byte array
     * @return created {@link Buffer}
     *
     * @since 0.4.0
     */
    @Deprecated
    public static Buffer copyOf(final byte[] src) {
        return copyOf(src, 0, src.length);
    }

    /**
     * Creates a new {@link Buffer} instance copied from {@code src}.
     *
     * @deprecated  This method is to be removed. Plugins should no longer call it directly.
     * @param src  the source byte array
     * @param index  starting position in the source array
     * @param length  the number of bytes to be copied
     * @return created {@link Buffer}
     *
     * @since 0.4.0
     */
    @Deprecated
    public static Buffer copyOf(final byte[] src, final int index, final int length) {
        return wrap(Arrays.copyOfRange(src, index, length));
    }

    /**
     * Creates a new {@link Buffer} instance wrapping {@code src} as the internal array.
     *
     * @deprecated  This method is to be removed. Plugins should no longer call it directly.
     * @param src  the source byte array
     * @return created {@link Buffer}
     *
     * @since 0.4.0
     */
    @Deprecated
    public static Buffer wrap(final byte[] src) {
        return wrap(src, 0, src.length);
    }

    /**
     * Creates a new {@link Buffer} instance wrapping {@code src} as the internal array.
     *
     * @deprecated  This method is to be removed. Plugins should no longer call it directly.
     * @param src  the source byte array
     * @param offset  starting position in the source array
     * @param size  the number of bytes to be wrapped
     * @return created {@link Buffer}
     *
     * @since 0.4.0
     */
    @Deprecated
    public static Buffer wrap(final byte[] src, final int offset, final int size) {
        try {
            return Holder.CONSTRUCTOR.newInstance(src, offset, size).limit(size);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("[FATAL] org.embulk.spi.BufferImpl is invalid.", ex);
        } catch (final InvocationTargetException ex) {
            throwCheckedForcibly(ex.getTargetException());
            return null;  // Should never reach.
        }
    }

    private static class Holder {  // Initialization-on-demand holder idiom.
        private static final Class<Buffer> IMPL_CLASS;
        private static final Constructor<Buffer> CONSTRUCTOR;

        static {
            try {
                IMPL_CLASS = loadBufferImpl();
            } catch (final ClassNotFoundException ex) {
                throw new LinkageError("[FATAL] org.embulk.spi.BufferImpl is not found.", ex);
            }

            try {
                CONSTRUCTOR = IMPL_CLASS.getConstructor(byte[].class, int.class, int.class);
            } catch (final NoSuchMethodException ex) {
                throw new LinkageError("[FATAL] org.embulk.spi.BufferImpl does not have an expected constructor.", ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<Buffer> loadBufferImpl() throws ClassNotFoundException {
        return (Class<Buffer>) CLASS_LOADER.loadClass("org.embulk.spi.BufferImpl");
    }

    private static void throwCheckedForcibly(final Throwable ex) {
        Buffer.<RuntimeException>throwCheckedForciblyInternal(ex);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwCheckedForciblyInternal(final Throwable ex) throws E {
        throw (E) ex;
    }

    private static final ClassLoader CLASS_LOADER = Buffer.class.getClassLoader();
}
