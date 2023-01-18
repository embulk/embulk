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
import java.util.Set;

/**
 * Represents an object in JSON.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format</a>
 *
 * @since 0.10.42
 */
public final class JsonObject extends AbstractMap<String, JsonValue> implements JsonValue {
    private JsonObject(final JsonValue[] values) {
        this.values = values;
    }

    /**
     * Returns a JSON object containing zero JSON key-value mapping.
     *
     * @return an empty JSON object
     *
     * @since 0.10.42
     */
    public static JsonObject of() {
        return EMPTY;
    }

    /**
     * Returns a JSON object containing an arbitrary number of JSON key-value mappings.
     *
     * <p>Keys and values are specified as varargs. Even numbers of arguments must be specified. Where <i>i</i> as an
     * odd number, the <i>i</i>-th argument represents a key that must be {@link JsonString}, and the <i>i+1</i>-th
     * argument represents a value for the key at the <i>i</i>-th argument.
     *
     * @param values  the JSON keys and values to be contained in the JSON object
     * @return a JSON object containing the specified JSON key-value mappings
     * @throws IllegalArgumentException  if odd numbers of arguments are specified, or non-{@link JsonString} is specified as key
     *
     * @since 0.10.42
     */
    public static JsonObject of(final JsonValue... values) {
        if (values.length == 0) {
            return EMPTY;
        }
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("Even numbers of arguments must be specified to JsonObject#of(...).");
        }
        for (int i = 0; i < values.length; i += 2) {
            if (!(values[i] instanceof JsonString)) {
                throw new IllegalArgumentException("JsonString must be specified as a key for JsonObject#of(...).");
            }
        }
        return new JsonObject(Arrays.copyOf(values, values.length));
    }

    /**
     * Returns a JSON object containing keys and values extracted from the given entries.
     *
     * <p>The specified entries themselves are not stored in the JSON object.
     *
     * @param entries  {@link java.util.Map.Entry}s containing {@link String} keys and {@link JsonValue} values from which the JSON object is populated
     * @return a JSON object containing the specified JSON key-value mappings
     *
     * @since 0.10.42
     */
    @SafeVarargs
    public static JsonObject ofEntries(final Map.Entry<String, JsonValue>... entries) {
        final JsonValue[] values = new JsonValue[entries.length * 2];
        for (int i = 0; i < entries.length; ++i) {
            values[i * 2] = JsonString.of(entries[i].getKey());
            values[i * 2 + 1] = entries[i].getValue();
        }
        return new JsonObject(values);
    }

    /**
     * Returns a JSON object containing keys and values extracted from the given entries.
     *
     * <p>The specified entries themselves are not stored in the JSON object.
     *
     * @param entries  {@link java.util.Map.Entry}s containing {@link JsonString} keys and {@link JsonValue} values from which the JSON object is populated
     * @return a JSON object containing the specified JSON key-value mappings
     *
     * @since 0.10.42
     */
    @SafeVarargs
    public static JsonObject ofEntriesWithJsonStringKeys(final Map.Entry<JsonString, JsonValue>... entries) {
        final JsonValue[] values = new JsonValue[entries.length * 2];
        for (int i = 0; i < entries.length; ++i) {
            values[i * 2] = entries[i].getKey();
            values[i * 2 + 1] = entries[i].getValue();
        }
        return new JsonObject(values);
    }

    /**
     * Returns a JSON object containing keys and values extracted from the given {@link java.util.Map}.
     *
     * <p>The specified map itself is not stored in the JSON object.
     *
     * @param map  a {@link java.util.Map} containing {@link String} keys and {@link JsonValue} values from which the JSON object is populated
     * @return a JSON object containing the specified JSON key-value mappings
     *
     * @since 0.10.42
     */
    public static JsonObject ofMap(final Map<String, JsonValue> map) {
        final JsonValue[] values = new JsonValue[map.size() * 2];
        int index = 0;
        for (final Map.Entry<String, JsonValue> pair : map.entrySet()) {
            values[index] = JsonString.of(pair.getKey());
            index++;
            values[index] = pair.getValue();
            index++;
        }
        return new JsonObject(values);
    }

    /**
     * Returns a JSON object containing keys and values extracted from the given {@link java.util.Map}.
     *
     * <p>The specified map itself is not stored in the JSON object.
     *
     * @param map  a {@link java.util.Map} containing {@link JsonString} keys and {@link JsonValue} values from which the JSON object is populated
     * @return a JSON object containing the specified JSON key-value mappings
     *
     * @since 0.10.42
     */
    public static JsonObject ofMapWithJsonStringKeys(final Map<JsonString, JsonValue> map) {
        final JsonValue[] values = new JsonValue[map.size() * 2];
        int index = 0;
        for (final Map.Entry<JsonString, JsonValue> pair : map.entrySet()) {
            values[index] = pair.getKey();
            index++;
            values[index] = pair.getValue();
            index++;
        }
        return new JsonObject(values);
    }

    /**
     * Returns a JSON object containing the specified array as its internal representation.
     *
     * <p><strong>This method is not safe.</strong> If the specified array is modified after creating a {@link JsonObject}
     * instance with this method, the created {@link JsonObject} instance can unintentionally behave differently.
     *
     * @param values  the array of JSON keys and values to be the internal representation as-is in the new {@link JsonObject}
     * @return a JSON object containing the specified array as the internal representation
     *
     * @since 0.10.42
     */
    public static JsonObject ofUnsafe(final JsonValue... values) {
        return new JsonObject(values);
    }

    /**
     * Returns a {@link JsonObject.Builder}.
     *
     * @return a {@link JsonObject.Builder}
     *
     * @since 0.10.42
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds instances of {@link JsonObject} from keys and values configured by {@code put*} methods.
     *
     * @since 0.10.42
     */
    public static class Builder {
        private Builder() {
            this.map = new LinkedHashMap<>();
        }

        /**
         * Returns a {@link JsonObject} that is built from keys and values configured by {@code put*} methods.
         *
         * @return a {@link JsonObject} that is built from keys and values configured by {@code put*} methods
         *
         * @since 0.10.42
         */
        public JsonObject build() {
            return JsonObject.ofMapWithJsonStringKeys(this.map);
        }

        /**
         * Puts a pair of a {@link String} key and a {@link JsonValue} value.
         *
         * @param key  the key
         * @param value  the value
         * @return this builder itself
         *
         * @since 0.10.42
         */
        public Builder put(final String key, final JsonValue value) {
            this.map.put(JsonString.of(key), value);
            return this;
        }

        /**
         * Puts a pair of a {@link JsonString} key and a {@link JsonValue} value.
         *
         * @param key  the key
         * @param value  the value
         * @return this builder itself
         *
         * @since 0.10.42
         */
        public Builder put(final JsonString key, final JsonValue value) {
            this.map.put(key, value);
            return this;
        }

        /**
         * Puts a {@link java.util.Map.Entry} of a {@link String} key and a {@link JsonValue} value.
         *
         * @param entry  an entry of a key and a value
         * @return this builder itself
         *
         * @since 0.10.42
         */
        public Builder putEntry(final Map.Entry<String, JsonValue> entry) {
            this.put(JsonString.of(entry.getKey()), entry.getValue());
            return this;
        }

        /**
         * Puts a {@link java.util.Map.Entry} of a {@link JsonString} key and a {@link JsonValue} value.
         *
         * @param entry  an entry of a key and a value
         * @return this builder itself
         *
         * @since 0.10.42
         */
        public Builder putEntryWithJsonStringKey(final Map.Entry<JsonString, JsonValue> entry) {
            this.put(entry.getKey(), entry.getValue());
            return this;
        }

        /**
         * Copies all of the JSON key-value mappings from the specified {@link java.util.Map} to this JSON object.
         *
         * @param map  a {@link java.util.Map}
         * @return this builder itself
         *
         * @since 0.10.42
         */
        public Builder putAll(final Map<String, JsonValue> map) {
            for (final Map.Entry<String, JsonValue> entry : map.entrySet()) {
                this.putEntry(entry);
            }
            return this;
        }

        /**
         * Copies all of the JSON key-value mappings from the specified {@link java.util.Map} to this JSON object.
         *
         * @param map  a {@link java.util.Map}
         * @return this builder itself
         *
         * @since 0.10.42
         */
        public Builder putAllWithJsonStringKeys(final Map<JsonString, JsonValue> map) {
            for (final Map.Entry<JsonString, JsonValue> entry : map.entrySet()) {
                this.putEntryWithJsonStringKey(entry);
            }
            return this;
        }

        private final LinkedHashMap<JsonString, JsonValue> map;
    }

    /**
     * Returns a {@link java.util.Map.Entry} of a {@link String} key and a {@link JsonValue} value.
     *
     * @param key  the key
     * @param value  the value
     * @return a {@link java.util.Map.Entry}
     *
     * @since 0.10.42
     */
    public static Map.Entry<String, JsonValue> entry(final String key, final JsonValue value) {
        return new AbstractMap.SimpleEntry<String, JsonValue>(key, value);
    }

    /**
     * Returns a {@link java.util.Map.Entry} of a {@link JsonString} key and a {@link JsonValue} value.
     *
     * @param key  the key
     * @param value  the value
     * @return a {@link java.util.Map.Entry}
     *
     * @since 0.10.42
     */
    public static Map.Entry<JsonString, JsonValue> entry(final JsonString key, final JsonValue value) {
        return new AbstractMap.SimpleEntry<JsonString, JsonValue>(key, value);
    }

    /**
     * Returns {@link JsonValue.Type#OBJECT}, which is the type of {@link JsonObject}.
     *
     * @return {@link JsonValue.Type#OBJECT}, which is the type of {@link JsonObject}
     *
     * @since 0.10.42
     */
    @Override
    public Type getType() {
        return Type.OBJECT;
    }

    /**
     * Returns this value as {@link JsonObject}.
     *
     * @return itself as {@link JsonObject}
     *
     * @since 0.10.42
     */
    @Override
    public JsonObject asJsonObject() {
        return this;
    }

    /**
     * Returns the number of JSON key-value mappings in this object.
     *
     * @return the number of JSON key-value mappings in this object
     *
     * @since 0.10.42
     */
    @Override
    public int size() {
        return this.values.length / 2;
    }

    /**
     * Returns a {@link java.util.Set} view of the JSON key-value mappings contained in this JSON object.
     *
     * <p>This JSON object is unmodifiable, then the set is unmodifiable, too.
     *
     * @return a {@link java.util.Set} view of the JSON value key-mappings contained in this JSON object
     *
     * @since 0.10.42
     */
    @Override
    public Set<Map.Entry<String, JsonValue>> entrySet() {
        return new EntrySet(this.values);
    }

    /**
     * Returns the key-value pairs as an array of {@code JsonValue}.
     *
     * Odd elements are keys. Next element of an odd element is a value corresponding to the key.
     *
     * For example, if this value represents <code>{"k1": "v1", "k2": "v2"}</code>, this method returns ["k1", "v1", "k2", "v2"].
     */
    public JsonValue[] getKeyValueArray() {
        return Arrays.copyOf(this.values, this.values.length);
    }

    /**
     * Returns the stringified JSON representation of this JSON object.
     *
     * @return the stringified JSON representation of this JSON object
     *
     * @since 0.10.42
     */
    @Override
    public String toJson() {
        if (this.values.length == 0) {
            return "{}";
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("{");
        if (this.values[0].isString()) {
            builder.append(this.values[0].toJson());
        } else {
            throw new IllegalStateException("Keys in JsonObject must be String.");
        }
        builder.append(":");
        builder.append(this.values[1].toJson());
        for (int i = 2; i < this.values.length; i += 2) {
            builder.append(",");
            if (this.values[i].isString()) {
                builder.append(this.values[i].toJson());
            } else {
                throw new IllegalStateException("Keys in JsonObject must be String.");
            }
            builder.append(":");
            builder.append(this.values[i + 1].toJson());
        }
        builder.append("}");
        return builder.toString();
    }

    /**
     * Returns the string representation of this JSON object.
     *
     * @return the string representation of this JSON object
     *
     * @since 0.10.42
     */
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

    /**
     * Compares the specified object with this JSON object for equality.
     *
     * @return {@code true} if the specified object is equal to this JSON object
     *
     * @since 0.10.42
     */
    @Override
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }
        if (!(otherObject instanceof JsonValue)) {
            return false;
        }

        final JsonValue otherValue = (JsonValue) otherObject;
        if (!otherValue.isObject()) {
            return false;
        }

        return this.entrySet().equals(otherValue.asJsonObject().entrySet());
    }

    /**
     * Returns the hash code value for this JSON object.
     *
     * @return the hash code value for this JSON object
     *
     * @since 0.10.42
     */
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

            if (!this.values[this.index].isString()) {
                throw new IllegalStateException("Keys in JsonObject must be String.");
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

    private static final JsonObject EMPTY = new JsonObject(new JsonValue[0]);

    private final JsonValue[] values;
}
