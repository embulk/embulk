package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;
import org.embulk.spi.json.JsonValue;
import org.embulk.spi.json.JsonValueImpl;
import org.msgpack.value.ImmutableValue;

/**
 * Page is an in-process (in-JVM) container of data records.
 *
 * It serializes records to byte[] (in org.embulk.spi.Buffer) in order to:
 * A) Avoid slowness by handling many Java Objects
 * B) Avoid complexity by type-safe primitive arrays
 * C) Track memory consumption by records
 * D) Use off-heap memory
 *
 * (C) and (D) may not be so meaningful as of v0.7+ (or since earlier) as recent Embulk unlikely
 * allocates so many Pages at the same time. Recent Embulk is streaming-driven instead of
 * multithreaded queue-based.
 *
 * Page is NOT for inter-process communication. For multi-process execution such as MapReduce
 * Executor, the executor plugin takes responsibility about interoperable serialization.
 */
public class PageImpl extends Page {
    private final Buffer buffer;
    private List<String> stringReferences;
    private List<JsonValue> jsonValueReferences;

    protected PageImpl(Buffer buffer) {
        this.buffer = buffer;
    }

    @SuppressWarnings("deprecation")  // Page.allocate(int) is deprecated.
    public static Page allocate(int length) {
        return new PageImpl(BufferImpl.allocate(length));
    }

    @SuppressWarnings("deprecation")  // Page.wrap(Buffer) is deprecated.
    public static Page wrap(Buffer buffer) {
        return new PageImpl(buffer);
    }

    @Override
    public Page setStringReferences(List<String> values) {
        this.stringReferences = values;
        return this;
    }

    /**
     * <p>It had @Override'd {@link org.embulk.spi.Page#setValueReferences(List)}, but it is deprecated now.
     * This method is now only for {@code embulk-core}'s internal use.
     */
    @Deprecated
    @SuppressWarnings("deprecation")  // Page#setValueReferences(List<ImmutableValue>) is deprecated.
    public Page setValueReferences(List<ImmutableValue> msgpackValues) {
        final ArrayList<JsonValue> jsonValueReferences = new ArrayList<>();
        for (final ImmutableValue msgpackValue : msgpackValues) {
            jsonValueReferences.add(JsonValueImpl.of(msgpackValue));
        }
        return this.setJsonValueReferences(jsonValueReferences);
    }

    public Page setJsonValueReferences(final List<JsonValue> jsonValues) {
        this.jsonValueReferences = jsonValues;
        return this;
    }

    @Override
    public List<String> getStringReferences() {
        // TODO used by mapreduce executor
        return stringReferences;
    }

    /**
     * <p>It had @Override'd {@link org.embulk.spi.Page#getValueReferences()}, but it is deprecated now.
     * This method is now only for {@code embulk-core}'s internal use.
     */
    @Deprecated
    @SuppressWarnings("deprecation")  // Page#getValueReferences() is deprecated.
    public List<ImmutableValue> getValueReferences() {
        // TODO used by mapreduce executor
        final ArrayList<ImmutableValue> msgpackValueReferences = new ArrayList<>();
        for (final JsonValue jsonValue : this.jsonValueReferences) {
            msgpackValueReferences.add(jsonValue.getMsgpackImmutableValue());
        }
        return msgpackValueReferences;
    }

    public List<JsonValue> getJsonValueReferences() {
        return this.jsonValueReferences;
    }

    @Override
    public String getStringReference(int index) {
        return stringReferences.get(index);
    }

    /**
     * <p>It had @Override'd {@link org.embulk.spi.Page#getValueReference(int)}, but it is deprecated now.
     */
    @Deprecated
    @SuppressWarnings("deprecation")  // Page#getValueReferences() is deprecated.
    public ImmutableValue getValueReference(int index) {
        return this.jsonValueReferences.get(index).getMsgpackImmutableValue();
    }

    @Override
    public JsonValue getJsonValueReference(int index) {
        return this.jsonValueReferences.get(index);
    }

    @Override
    public void release() {
        buffer.release();
    }

    @Override
    public Buffer buffer() {
        return buffer;
    }
}
