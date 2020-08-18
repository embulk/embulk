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
public class Page {
    private final Buffer buffer;
    private List<String> stringReferences;
    private List<ImmutableValue> valueReferences;

    protected Page(Buffer buffer) {
        this.buffer = buffer;
    }

    public static Page allocate(int length) {
        return new Page(BufferImpl.allocate(length));
    }

    public static Page wrap(Buffer buffer) {
        return new Page(buffer);
    }

    public Page setStringReferences(List<String> values) {
        this.stringReferences = values;
        return this;
    }

    public Page setValueReferences(List<ImmutableValue> values) {
        this.valueReferences = values;
        return this;
    }

    public List<String> getStringReferences() {
        // TODO used by mapreduce executor
        return stringReferences;
    }

    public List<ImmutableValue> getValueReferences() {
        // TODO used by mapreduce executor
        return valueReferences;
    }

    public String getStringReference(int index) {
        return stringReferences.get(index);
    }

    public ImmutableValue getValueReference(int index) {
        return valueReferences.get(index);
    }

    public void release() {
        buffer.release();
    }

    public Buffer buffer() {
        return buffer;
    }
}
