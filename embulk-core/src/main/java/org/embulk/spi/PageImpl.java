package org.embulk.spi;

import java.util.List;
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
    private List<ImmutableValue> valueReferences;
    private final LineageMetadata pageLineage;
    //private final List<LineageMetadata> recordLineage;

    protected PageImpl(Buffer buffer) {
        this.buffer = buffer;
        this.pageLineage = LineageMetadata.EMPTY;
        // this.recordLineage = null;
    }

    PageImpl(final Buffer buffer, final LineageMetadata pageLineage, final List<LineageMetadata> recordLineage) {
        this.buffer = buffer;
        this.pageLineage = pageLineage;
        //this.recordLineage = ;
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

    @Override
    public Page setValueReferences(List<ImmutableValue> values) {
        this.valueReferences = values;
        return this;
    }

    @Override
    public List<String> getStringReferences() {
        // TODO used by mapreduce executor
        return stringReferences;
    }

    @Override
    public List<ImmutableValue> getValueReferences() {
        // TODO used by mapreduce executor
        return valueReferences;
    }

    @Override
    public String getStringReference(int index) {
        return stringReferences.get(index);
    }

    @Override
    public ImmutableValue getValueReference(int index) {
        return valueReferences.get(index);
    }

    // @Override
    public LineageMetadata getPageLineage() {
        return this.pageLineage;
    }

    // @Override
    public LineageMetadata getRecordLineage(final int recordIndex) {
        return this.pageLineage;
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
