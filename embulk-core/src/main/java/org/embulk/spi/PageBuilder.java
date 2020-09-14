package org.embulk.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import org.msgpack.value.Value;

public class PageBuilder implements AutoCloseable {
    PageBuilder() {
        this.delegate = null;
    }

    private PageBuilder(final PageBuilder delegate) {
        this.delegate = delegate;
    }

    /**
     * Constructs a {@link PageBuilder} instance.
     *
     * @deprecated The constructor is deprecated although Embulk v0.9-compatible plugins still have to use this.
     *     See <a href="https://github.com/embulk/embulk/issues/1321">GitHub Issue #1321: Deprecate PageBuilder's constructor</a>
     *     for the details.
     */
    @Deprecated  // https://github.com/embulk/embulk/issues/1321
    public PageBuilder(BufferAllocator allocator, Schema schema, PageOutput output) {
        this(createImplInstance(allocator, schema, output));
    }

    public Schema getSchema() {
        return this.delegate.getSchema();
    }

    public void setNull(Column column) {
        this.delegate.setNull(column);
    }

    public void setNull(int columnIndex) {
        this.delegate.setNull(columnIndex);
    }

    public void setBoolean(Column column, boolean value) {
        this.delegate.setBoolean(column, value);
    }

    public void setBoolean(int columnIndex, boolean value) {
        this.delegate.setBoolean(columnIndex, value);
    }

    public void setLong(Column column, long value) {
        this.delegate.setLong(column, value);
    }

    public void setLong(int columnIndex, long value) {
        this.delegate.setLong(columnIndex, value);
    }

    public void setDouble(Column column, double value) {
        this.delegate.setDouble(column, value);
    }

    public void setDouble(int columnIndex, double value) {
        this.delegate.setDouble(columnIndex, value);
    }

    public void setString(Column column, String value) {
        this.delegate.setString(column, value);
    }

    public void setString(int columnIndex, String value) {
        this.delegate.setString(columnIndex, value);
    }

    public void setJson(Column column, Value value) {
        this.delegate.setJson(column, value);
    }

    public void setJson(int columnIndex, Value value) {
        this.delegate.setJson(columnIndex, value);
    }

    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void setTimestamp(Column column, org.embulk.spi.time.Timestamp value) {
        this.delegate.setTimestamp(column, value);
    }

    public void setTimestamp(final Column column, final Instant value) {
        this.delegate.setTimestamp(column, value);
    }

    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void setTimestamp(int columnIndex, org.embulk.spi.time.Timestamp value) {
        this.delegate.setTimestamp(columnIndex, value);
    }

    public void setTimestamp(final int columnIndex, final Instant value) {
        this.delegate.setTimestamp(columnIndex, value);
    }

    public void addRecord() {
        this.delegate.addRecord();
    }

    public void flush() {
        this.delegate.flush();
    }

    public void finish() {
        this.delegate.finish();
    }

    @Override
    public void close() {
        this.delegate.close();
    }

    private static PageBuilder createImplInstance(final BufferAllocator allocator, final Schema schema, final PageOutput output) {
        try {
            return Holder.CONSTRUCTOR.newInstance(allocator, schema, output);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("[FATAL] org.embulk.spi.PageBuilderImpl is invalid.", ex);
        } catch (final InvocationTargetException ex) {
            throwCheckedForcibly(ex.getTargetException());
            return null;  // Should never reach.
        }
    }

    private static class Holder {  // Initialization-on-demand holder idiom.
        private static final Class<PageBuilder> IMPL_CLASS;
        private static final Constructor<PageBuilder> CONSTRUCTOR;

        static {
            try {
                IMPL_CLASS = loadPageBuilderImpl();
            } catch (final ClassNotFoundException ex) {
                throw new LinkageError("[FATAL] org.embulk.spi.PageBuilderImpl is not found.", ex);
            }

            try {
                CONSTRUCTOR = IMPL_CLASS.getConstructor(BufferAllocator.class, Schema.class, PageOutput.class);
            } catch (final NoSuchMethodException ex) {
                throw new LinkageError("[FATAL] org.embulk.spi.PageBuilderImpl does not have an expected constructor.", ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<PageBuilder> loadPageBuilderImpl() throws ClassNotFoundException {
        return (Class<PageBuilder>) CLASS_LOADER.loadClass("org.embulk.spi.PageBuilderImpl");
    }

    private static void throwCheckedForcibly(final Throwable ex) {
        PageBuilder.<RuntimeException>throwCheckedForciblyInternal(ex);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwCheckedForciblyInternal(final Throwable ex) throws E {
        throw (E) ex;
    }

    private static final ClassLoader CLASS_LOADER = PageBuilder.class.getClassLoader();

    private final PageBuilder delegate;
}
