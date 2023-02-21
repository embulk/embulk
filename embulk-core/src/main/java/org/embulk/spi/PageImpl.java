package org.embulk.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.embulk.spi.json.JsonValue;
import org.msgpack.value.ImmutableValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static PageImpl allocate(int length) {
        return new PageImpl(BufferImpl.allocate(length));
    }

    @SuppressWarnings("deprecation")  // Page.wrap(Buffer) is deprecated.
    public static PageImpl wrap(Buffer buffer) {
        return new PageImpl(buffer);
    }

    @Deprecated
    @Override
    public Page setStringReferences(List<String> values) {
        warn("Page#setStringReferences(List<String>)", hasLoggedSetStringReferences);
        return this.setStringReferencesInternal(values);
    }

    PageImpl setStringReferencesInternal(final List<String> values) {
        this.stringReferences = values;
        return this;
    }

    @Deprecated
    @Override
    @SuppressWarnings("deprecation")
    public Page setValueReferences(List<ImmutableValue> values) {
        warn("Page#setValueReferences(List<ImmutableValue>)", hasLoggedSetValueReferences);
        final ArrayList<JsonValue> newList = new ArrayList<>();
        for (final ImmutableValue msgpackValue : values) {
            newList.add(JsonValue.fromMsgpack(msgpackValue));
        }
        return this.setJsonValueReferencesInternal(Collections.unmodifiableList(newList));
    }

    PageImpl setJsonValueReferencesInternal(final List<JsonValue> values) {
        this.jsonValueReferences = values;
        return this;
    }

    @Deprecated
    @Override
    public List<String> getStringReferences() {
        warn("Page#getStringReferences()", hasLoggedGetStringReferences);
        return this.getStringReferencesInternal();
    }

    List<String> getStringReferencesInternal() {
        return this.stringReferences;
    }

    @Deprecated
    @Override
    public List<ImmutableValue> getValueReferences() {
        warn("Page#getValueReferences()", hasLoggedGetValueReferences);
        final ArrayList<ImmutableValue> msgpackValueReferences = new ArrayList<>();
        for (final JsonValue jsonValue : this.getJsonValueReferencesInternal()) {
            msgpackValueReferences.add(jsonValue.toMsgpack().immutableValue());
        }
        return Collections.unmodifiableList(msgpackValueReferences);
    }

    List<JsonValue> getJsonValueReferencesInternal() {
        return this.jsonValueReferences;
    }

    @Override
    public String getStringReference(int index) {
        return stringReferences.get(index);
    }

    @Deprecated
    @Override
    public ImmutableValue getValueReference(int index) {
        warn("Page#getValueReference()", hasLoggedGetValueReference);
        return this.getJsonValueReference(index).toMsgpack().immutableValue();
    }

    @Override
    public JsonValue getJsonValueReference(final int index) {
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

    private static class Warning extends RuntimeException {
        Warning(final String methodName) {
            super("Page#" + methodName + " is called.");
        }
    }

    private static void warn(final String methodName, final AtomicBoolean hasLogged) {
        if (!hasLogged.getAndSet(true)) {
            logger.debug("{} is called.", methodName);
        } else {
            logger.info(methodName + " is called.", new Warning(methodName + " is called."));
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(PageImpl.class);

    private static final AtomicBoolean hasLoggedSetStringReferences = new AtomicBoolean(false);

    private static final AtomicBoolean hasLoggedSetValueReferences = new AtomicBoolean(false);

    private static final AtomicBoolean hasLoggedGetStringReferences = new AtomicBoolean(false);

    private static final AtomicBoolean hasLoggedGetValueReferences = new AtomicBoolean(false);

    private static final AtomicBoolean hasLoggedGetValueReference = new AtomicBoolean(false);
}
