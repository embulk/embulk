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
import java.util.List;
import org.embulk.spi.json.JsonValue;
import org.msgpack.value.ImmutableValue;

/**
 * An in-process (in-JVM) container of data records.
 *
 * <p>It serializes records to {@code byte[]} (in {@link org.embulk.spi.Buffer}) for the following purposes.
 *
 * <ul>
 * <li>A) Avoid slowness by handling many Java Objects
 * <li>B) Avoid complexity by type-safe primitive arrays
 * <li>C) Track memory consumption by records
 * <li>D) Use off-heap memory
 * </ul>
 *
 * <p>(C) and (D) may not be so meaningful as of Embulk v0.7+ (or since earlier) as recent Embulk unlikely
 * allocates so many {@link Page}s at the same time. Recent Embulk is streaming-driven instead of
 * multithreaded queue-based.
 *
 * <p>{@link Page} is NOT for inter-process communication. For multi-process execution such as the deprecated
 * MapReduce Executor, the executor plugin takes responsibility about interoperable serialization.
 *
 * @since 0.4.0
 */
public abstract class Page {
    /**
     * Sets the list of string references in this {@link Page}.
     *
     * @param values  the list of string references to set
     * @return this {@link Page} itself
     * @deprecated Do not call this method directly from plugins.
     *
     * @since 0.4.0
     */
    @Deprecated
    public abstract Page setStringReferences(List<String> values);

    /**
     * Sets the list of JSON value references in this {@link Page} in the {@code msgpack-java} representation.
     *
     * <p>The JSON values are converted to {@link org.embulk.spi.json.JsonValue} from the {@code msgpack-java} representation.
     *
     * @param values  the list of JSON value references to set in the {@code msgpack-java} representation
     * @return this {@link Page} itself
     * @deprecated Do not call this method directly from plugins.
     *
     * @since 0.8.0
     */
    @Deprecated
    public abstract Page setValueReferences(List<ImmutableValue> values);

    /**
     * Returns the list of string references in this {@link Page}.
     *
     * @return the list of string references in this {@link Page}
     * @deprecated Do not call this method directly from plugins.
     *
     * @since 0.6.0
     */
    @Deprecated
    public abstract List<String> getStringReferences();

    /**
     * Returns the list of JSON value references in this {@link Page} in the {@code msgpack-java} representation.
     *
     * <p>The JSON values are converted to the {@code msgpack-java} representation from {@link org.embulk.spi.json.JsonValue} in this {@link Page}.
     *
     * @return the list of JSON value references in this {@link Page} in the {@code msgpack-java} representation
     * @deprecated Do not call this method directly from plugins.
     *
     * @since 0.8.0
     */
    @Deprecated
    public abstract List<ImmutableValue> getValueReferences();

    /**
     * Returns a string from the string references in this {@link Page} at {@code index}.
     *
     * @param index  the index of the string reference to return
     * @return the string from the string references in this {@link Page} at {@code index}
     *
     * @since 0.4.0
     */
    public abstract String getStringReference(int index);

    /**
     * Returns a JSON value from the JSON value references in this {@link Page} at {@code index} in the {@code msgpack-java} representation.
     *
     * <p>The JSON value is converted to the {@code msgpack-java} representatino from {@link org.embulk.spi.json.JsonValue} in this {@link Page}.
     *
     * @param index  the index of the JSON value reference to return
     * @return the JSON value from the JSON value references in this {@link Page} at {@code index} in the {@code msgpack-java} representation
     * @deprecated Use {@link #getJsonValueReference(int)} instead.
     *
     * @since 0.8.0
     */
    @Deprecated
    public abstract ImmutableValue getValueReference(int index);

    /**
     * Returns a JSON value from the JSON value references in this {@link Page} at {@code index}.
     *
     * @param index  the index of the JSON value reference to return
     * @return the JSON value from the JSON value references in this {@link Page} at {@code index}
     *
     * @since 0.10.42
     */
    public abstract JsonValue getJsonValueReference(int index);

    /**
     * @since 0.4.0
     */
    public abstract void release();

    /**
     * @since 0.4.0
     */
    public abstract Buffer buffer();

    /**
     * Creates a new {@link Page} instance.
     *
     * @deprecated It is to be removed, implemented only for compatibility. Plugins should no longer call it directly.
     * @param length  length of the internal {@link Buffer} of the created {@link Page}
     * @return {@link Page} created
     *
     * @since 0.4.0
     */
    @Deprecated
    public static Page allocate(final int length) {
        final Buffer buffer;
        try {
            buffer = BufferImplHolder.CONSTRUCTOR.newInstance(new byte[length], 0, length);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("[FATAL] org.embulk.spi.BufferImpl is invalid.", ex);
        } catch (final InvocationTargetException ex) {
            throwCheckedForcibly(ex.getTargetException());
            return null;  // Should never reach.
        }

        try {
            return PageImplHolder.CONSTRUCTOR.newInstance(buffer);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("[FATAL] org.embulk.spi.PageImpl is invalid.", ex);
        } catch (final InvocationTargetException ex) {
            throwCheckedForcibly(ex.getTargetException());
            return null;  // Should never reach.
        }
    }

    /**
     * Creates a new {@link Page} instance wrapping an internal {@link Buffer}.
     *
     * @deprecated It is to be removed, implemented only for compatibility. Plugins should no longer call it directly.
     * @param buffer  the internal {@link Buffer}
     * @return {@link Page} created
     *
     * @since 0.4.0
     */
    @Deprecated
    public static Page wrap(final Buffer buffer) {
        try {
            return PageImplHolder.CONSTRUCTOR.newInstance(buffer);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("[FATAL] org.embulk.spi.PageImpl is invalid.", ex);
        } catch (final InvocationTargetException ex) {
            throwCheckedForcibly(ex.getTargetException());
            return null;  // Should never reach.
        }
    }

    private static class BufferImplHolder {  // Initialization-on-demand holder idiom.
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

    private static class PageImplHolder {  // Initialization-on-demand holder idiom.
        private static final Class<Page> IMPL_CLASS;
        private static final Constructor<Page> CONSTRUCTOR;

        static {
            try {
                IMPL_CLASS = loadPageImpl();
            } catch (final ClassNotFoundException ex) {
                throw new LinkageError("[FATAL] org.embulk.spi.PageImpl is not found.", ex);
            }

            try {
                CONSTRUCTOR = IMPL_CLASS.getConstructor(Buffer.class);
            } catch (final NoSuchMethodException ex) {
                throw new LinkageError("[FATAL] org.embulk.spi.PageImpl does not have an expected constructor.", ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<Buffer> loadBufferImpl() throws ClassNotFoundException {
        return (Class<Buffer>) CLASS_LOADER.loadClass("org.embulk.spi.BufferImpl");
    }

    @SuppressWarnings("unchecked")
    private static Class<Page> loadPageImpl() throws ClassNotFoundException {
        return (Class<Page>) CLASS_LOADER.loadClass("org.embulk.spi.PageImpl");
    }

    private static void throwCheckedForcibly(final Throwable ex) {
        Page.<RuntimeException>throwCheckedForciblyInternal(ex);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwCheckedForciblyInternal(final Throwable ex) throws E {
        throw (E) ex;
    }

    private static final ClassLoader CLASS_LOADER = Page.class.getClassLoader();
}
