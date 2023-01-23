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
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public final class FakeJsonObject extends AbstractMap<String, JsonValue> implements JsonValue {
    private FakeJsonObject(final String[] keys, final JsonValue[] values) {
        this.keys = keys;
        this.values = values;
    }

    public static FakeJsonObject of(final JsonValue... keyValues) {
        if (keyValues.length == 0) {
            return EMPTY;
        }
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Even numbers of arguments must be specified to JsonObject#of(...).");
        }
        final String[] keys = new String[keyValues.length / 2];
        final JsonValue[] values = new JsonValue[keyValues.length / 2];

        for (int i = 0; i < keyValues.length / 2; i++) {
            if (!(keyValues[i * 2] instanceof JsonString)) {
                throw new IllegalArgumentException("JsonString must be specified as a key for JsonObject#of(...).");
            }
            keys[i] = ((JsonString) keyValues[i * 2]).getString();
            values[i] = keyValues[i * 2 + 1];
        }
        return new FakeJsonObject(keys, values);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OBJECT;
    }

    @Override
    public int presumeReferenceSizeInBytes() {
        int sum = 4;
        for (int i = 0; i < this.values.length; i++) {
            sum += this.keys[i].length() * 2 + 4;
            sum += this.values[i].presumeReferenceSizeInBytes();
        }
        return sum;
    }

    @Override
    public int size() {
        return this.values.length;
    }

    @Override
    public Set<Map.Entry<String, JsonValue>> entrySet() {
        return new EntrySet(this.keys, this.values);
    }

    public JsonValue[] getKeyValueArray() {
        final JsonValue[] keyValues = new JsonValue[this.keys.length * 2];
        for (int i = 0; i < this.keys.length; i++) {
            keyValues[i * 2] = JsonString.of(this.keys[i]);
            keyValues[i * 2 + 1] = this.values[i];
        }
        return keyValues;
    }

    @Override
    public String toJson() {
        if (this.keys.length == 0) {
            return "{}";
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(JsonString.escapeStringForJsonLiteral(this.keys[0]));
        builder.append(":");
        builder.append(this.values[0].toJson());
        for (int i = 1; i < this.keys.length; i++) {
            builder.append(",");
            builder.append(JsonString.escapeStringForJsonLiteral(this.keys[i]));
            builder.append(":");
            builder.append(this.values[i].toJson());
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public String toString() {
        if (this.keys.length == 0) {
            return "{}";
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(JsonString.escapeStringForJsonLiteral(this.keys[0]));
        builder.append(":");
        builder.append(this.values[0].toString());
        for (int i = 1; i < this.keys.length; i++) {
            builder.append(",");
            builder.append(JsonString.escapeStringForJsonLiteral(this.keys[i]));
            builder.append(":");
            builder.append(this.values[i].toString());
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }

        // Fake!
        if (otherObject instanceof JsonObject) {
            final JsonObject other = (JsonObject) otherObject;
            return Objects.equals(this.entrySet(), other.entrySet());
        }

        // Check by `instanceof` in case against unexpected arbitrary extension of JsonValue.
        if (!(otherObject instanceof FakeJsonObject)) {
            return false;
        }

        final FakeJsonObject other = (FakeJsonObject) otherObject;

        // The equality of JsonObject should be checked like a Map, not by the internal key-value array.
        // For example, the order of the internal key-value array should not impact the equality of JsonObject.
        return Objects.equals(this.entrySet(), other.entrySet());
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < this.keys.length; i++) {
            hash += this.keys[i].hashCode() ^ this.values[i].hashCode();
        }
        return hash;
    }

    private static class EntrySet extends AbstractSet<Map.Entry<String, JsonValue>> {
        EntrySet(final String[] keys, final JsonValue[] values) {
            this.keys = keys;
            this.values = values;
        }

        @Override
        public int size() {
            return this.keys.length;
        }

        @Override
        public Iterator<Map.Entry<String, JsonValue>> iterator() {
            return new EntryIterator(this.keys, this.values);
        }

        private final String[] keys;
        private final JsonValue[] values;
    }

    private static class EntryIterator implements Iterator<Map.Entry<String, JsonValue>> {
        EntryIterator(final String[] keys, final JsonValue[] values) {
            this.keys = keys;
            this.values = values;
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.keys.length;
        }

        @Override
        public Map.Entry<String, JsonValue> next() {
            if (this.index >= this.values.length) {
                throw new NoSuchElementException();
            }

            final String key = this.keys[index];
            final JsonValue value = this.values[index];
            final Map.Entry<String, JsonValue> pair = new AbstractMap.SimpleImmutableEntry<>(key, value);

            this.index++;
            return pair;
        }

        private final String[] keys;
        private final JsonValue[] values;

        private int index;
    }

    private static final FakeJsonObject EMPTY = new FakeJsonObject(new String[0], new JsonValue[0]);

    private final String[] keys;
    private final JsonValue[] values;
}
