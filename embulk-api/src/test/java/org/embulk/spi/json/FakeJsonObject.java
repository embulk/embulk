/*
 * Copyright 2022 The Embulk project
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

package org.embulk.spi.json;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public final class FakeJsonObject extends AbstractMap<String, JsonValue> implements JsonValue {
    private FakeJsonObject(final JsonValue[] values) {
        this.values = values;
    }

    public static FakeJsonObject of(final JsonValue... values) {
        if (values.length == 0) {
            return EMPTY;
        }
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("Even numbers of arguments must be specified to FakeJsonObject#of(...).");
        }
        for (int i = 0; i < values.length; i += 2) {
            if (!(values[i] instanceof JsonString)) {
                throw new IllegalArgumentException("JsonString must be specified as a key for FakeJsonObject#of(...).");
            }
        }
        return new FakeJsonObject(Arrays.copyOf(values, values.length));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OBJECT;
    }

    @Override
    public int size() {
        return this.values.length / 2;
    }

    @Override
    public Set<Map.Entry<String, JsonValue>> entrySet() {
        return new EntrySet(this.values);
    }

    public JsonValue[] getKeyValueArray() {
        return Arrays.copyOf(this.values, this.values.length);
    }

    @Override
    public String toJson() {
        if (this.values.length == 0) {
            return "{}";
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("{");
        if (this.values[0].isJsonString()) {
            builder.append(this.values[0].toJson());
        } else {
            throw new IllegalStateException("Keys in FakeJsonObject must be String.");
        }
        builder.append(":");
        builder.append(this.values[1].toJson());
        for (int i = 2; i < this.values.length; i += 2) {
            builder.append(",");
            if (this.values[i].isJsonString()) {
                builder.append(this.values[i].toJson());
            } else {
                throw new IllegalStateException("Keys in FakeJsonObject must be String.");
            }
            builder.append(":");
            builder.append(this.values[i + 1].toJson());
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public String toString() {
        if (this.values.length == 0) {
            return "{}";
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(this.values[0].toString());
        builder.append(":");
        builder.append(this.values[1].toString());
        for (int i = 2; i < this.values.length; i += 2) {
            builder.append(",");
            builder.append(this.values[i].toString());
            builder.append(":");
            builder.append(this.values[i + 1].toString());
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }
        if (!(otherObject instanceof JsonValue)) {
            return false;
        }

        final JsonValue otherValue = (JsonValue) otherObject;
        if (!otherValue.isJsonObject()) {
            return false;
        }

        return this.entrySet().equals(otherValue.asJsonObject().entrySet());
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < this.values.length; i += 2) {
            hash += this.values[i].hashCode() ^ this.values[i + 1].hashCode();
        }
        return hash;
    }

    private static class EntrySet extends AbstractSet<Map.Entry<String, JsonValue>> {
        EntrySet(final JsonValue[] values) {
            this.values = values;
        }

        @Override
        public int size() {
            return this.values.length / 2;
        }

        @Override
        public Iterator<Map.Entry<String, JsonValue>> iterator() {
            return new EntryIterator(this.values);
        }

        private final JsonValue[] values;
    }

    private static class EntryIterator implements Iterator<Map.Entry<String, JsonValue>> {
        EntryIterator(final JsonValue[] values) {
            this.values = values;
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.values.length;
        }

        @Override
        public Map.Entry<String, JsonValue> next() {
            if (this.index >= this.values.length) {
                throw new NoSuchElementException();
            }

            if (!this.values[this.index].isJsonString()) {
                throw new IllegalStateException("Keys in FakeJsonObject must be String.");
            }
            final String key = ((JsonString) this.values[index]).getString();
            final JsonValue value = this.values[index + 1];
            final Map.Entry<String, JsonValue> pair = new AbstractMap.SimpleImmutableEntry<>(key, value);

            this.index += 2;
            return pair;
        }

        private final JsonValue[] values;

        private int index;
    }

    private static final FakeJsonObject EMPTY = new FakeJsonObject(new JsonValue[0]);

    private final JsonValue[] values;
}
