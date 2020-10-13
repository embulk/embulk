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
import java.lang.reflect.Method;
import java.time.Instant;
import org.msgpack.value.Value;

/**
 * Reads data records from {@link Page}s set with {@link #setPage(Page)}.
 *
 * @since 0.4.0
 */
public class PageReader implements AutoCloseable {
    PageReader() {
        this.delegate = null;
    }

    private PageReader(final PageReader delegate) {
        this.delegate = delegate;
    }

    /**
     * Constructs a {@link PageReader} instance.
     *
     * @deprecated The constructor is deprecated although Embulk v0.9-compatible plugins still have to use this.
     *     See <a href="https://github.com/embulk/embulk/issues/1323">GitHub Issue #1323: Deprecate PageReader's constructor</a>
     *     for the details.
     *
     * @since 0.4.0
     */
    public PageReader(Schema schema) {
        this(createImplInstance(schema));
    }

    /**
     * @since 0.4.0
     */
    public static int getRecordCount(Page page) {
        return callGetRecordCount(page);
    }

    /**
     * @since 0.4.0
     */
    public void setPage(Page page) {
        this.delegate.setPage(page);
    }

    /**
     * @since 0.4.0
     */
    public Schema getSchema() {
        return this.delegate.getSchema();
    }

    /**
     * @since 0.4.0
     */
    public boolean isNull(Column column) {
        return this.delegate.isNull(column);
    }

    /**
     * @since 0.4.0
     */
    public boolean isNull(int columnIndex) {
        return this.delegate.isNull(columnIndex);
    }

    /**
     * @since 0.4.0
     */
    public boolean getBoolean(Column column) {
        return this.delegate.getBoolean(column);
    }

    /**
     * @since 0.4.0
     */
    public boolean getBoolean(int columnIndex) {
        return this.delegate.getBoolean(columnIndex);
    }

    /**
     * @since 0.4.0
     */
    public long getLong(Column column) {
        return this.delegate.getLong(column);
    }

    /**
     * @since 0.4.0
     */
    public long getLong(int columnIndex) {
        return this.delegate.getLong(columnIndex);
    }

    /**
     * @since 0.4.0
     */
    public double getDouble(Column column) {
        return this.delegate.getDouble(column);
    }

    /**
     * @since 0.4.0
     */
    public double getDouble(int columnIndex) {
        return this.delegate.getDouble(columnIndex);
    }

    /**
     * @since 0.4.0
     */
    public String getString(Column column) {
        return this.delegate.getString(column);
    }

    /**
     * @since 0.4.0
     */
    public String getString(int columnIndex) {
        return this.delegate.getString(columnIndex);
    }

    /**
     * Returns a Timestamp value.
     *
     * @deprecated Use {@link #getTimestampInstant(Column)} instead.
     *
     * @since 0.4.0
     */
    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public org.embulk.spi.time.Timestamp getTimestamp(Column column) {
        return this.delegate.getTimestamp(column);
    }

    /**
     * Returns a Timestamp value.
     *
     * @deprecated Use {@link #getTimestampInstant(int)} instead.
     *
     * @since 0.4.0
     */
    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public org.embulk.spi.time.Timestamp getTimestamp(int columnIndex) {
        return this.delegate.getTimestamp(columnIndex);
    }

    /**
     * @since 0.10.13
     */
    public Instant getTimestampInstant(final Column column) {
        return this.delegate.getTimestampInstant(column);
    }

    /**
     * @since 0.10.13
     */
    public Instant getTimestampInstant(final int columnIndex) {
        return this.delegate.getTimestampInstant(columnIndex);
    }

    /**
     * @since 0.8.0
     */
    public Value getJson(Column column) {
        return this.delegate.getJson(column);
    }

    /**
     * @since 0.8.0
     */
    public Value getJson(int columnIndex) {
        return this.delegate.getJson(columnIndex);
    }

    /**
     * @since 0.4.0
     */
    public boolean nextRecord() {
        return this.delegate.nextRecord();
    }

    /**
     * @since 0.4.0
     */
    @Override
    public void close() {
        this.delegate.close();
    }

    private static PageReader createImplInstance(final Schema schema) {
        try {
            return Holder.CONSTRUCTOR.newInstance(schema);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("[FATAL] org.embulk.spi.PageReaderImpl is invalid.", ex);
        } catch (final InvocationTargetException ex) {
            throwCheckedForcibly(ex.getTargetException());
            return null;  // Should never reach.
        }
    }

    private static int callGetRecordCount(final Page page) {
        final Object result;
        try {
            result = Holder.GET_RECORD_COUNT.invoke(null, page);
        } catch (final IllegalAccessException | IllegalArgumentException ex) {
            throw new LinkageError("[FATAL] org.embulk.spi.PageReaderImpl is invalid.", ex);
        } catch (final InvocationTargetException ex) {
            throwCheckedForcibly(ex.getTargetException());
            return Integer.MIN_VALUE;  // Should never reach.
        }

        if (result == null) {
            throw new LinkageError(
                    "[FATAL] org.embulk.spi.PageReaderImpl is invalid.",
                    new NullPointerException("PageReaderImpl#getRecordCount returned null."));
        }
        if (!(result instanceof Integer)) {
            throw new LinkageError(
                    "[FATAL] org.embulk.spi.PageReaderImpl is invalid.",
                    new ClassCastException("PageReaderImpl#getRecordCount returned non-int."));
        }
        return (Integer) result;
    }

    private static class Holder {  // Initialization-on-demand holder idiom.
        private static final Class<PageReader> IMPL_CLASS;
        private static final Constructor<PageReader> CONSTRUCTOR;
        private static final Method GET_RECORD_COUNT;

        static {
            try {
                IMPL_CLASS = loadPageReaderImpl();
            } catch (final ClassNotFoundException ex) {
                throw new LinkageError("[FATAL] org.embulk.spi.PageReaderImpl is not found.", ex);
            }

            try {
                CONSTRUCTOR = IMPL_CLASS.getConstructor(Schema.class);
            } catch (final NoSuchMethodException ex) {
                throw new LinkageError("[FATAL] org.embulk.spi.PageReaderImpl does not have an expected constructor.", ex);
            }

            try {
                GET_RECORD_COUNT = IMPL_CLASS.getMethod("getRecordCount", Page.class);
            } catch (final NoSuchMethodException ex) {
                throw new LinkageError("[FATAL] org.embulk.spi.PageReaderImpl does not have a static method 'getRecordCount'.", ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<PageReader> loadPageReaderImpl() throws ClassNotFoundException {
        return (Class<PageReader>) CLASS_LOADER.loadClass("org.embulk.spi.PageReaderImpl");
    }

    private static void throwCheckedForcibly(final Throwable ex) {
        PageReader.<RuntimeException>throwCheckedForciblyInternal(ex);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwCheckedForciblyInternal(final Throwable ex) throws E {
        throw (E) ex;
    }

    private static final ClassLoader CLASS_LOADER = PageReader.class.getClassLoader();

    private final PageReader delegate;
}
