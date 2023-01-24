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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Represents an object in JSON.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format</a>
 *
 * @since 0.10.42
 */
public final class JsonObject extends AbstractMap<String, JsonValue> implements JsonValue {
    private JsonObject(final String[] keys, final JsonValue[] values) {
        this.keys = keys;
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
     * Returns a JSON object containing the specified single JSON key-value mapping.
     *
     * @param k1  the mapping's key, not null
     * @param v1  the mapping's value, not null
     * @return a JSON object containing the specified mapping
     * @throws NullPointerException  if the key or the value is {@code null}
     *
     * @since 0.10.42
     */
    public static JsonObject of(
            final String k1, final JsonValue v1) {
        return new JsonObject(
                buildKeys(k1),
                buildValues(v1));
    }

    /**
     * Returns a JSON object containing the specified two JSON key-value mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value, not null
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value, not null
     * @return a JSON object containing the specified mappings
     * @throws NullPointerException  if any key or any value is {@code null}
     *
     * @since 0.10.42
     */
    public static JsonObject of(
            final String k1, final JsonValue v1,
            final String k2, final JsonValue v2) {
        return new JsonObject(
                buildKeys(k1, k2),
                buildValues(v1, v2));
    }

    /**
     * Returns a JSON object containing the specified three JSON key-value mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value, not null
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value, not null
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value, not null
     * @return a JSON object containing the specified mappings
     * @throws NullPointerException  if any key or any value is {@code null}
     *
     * @since 0.10.42
     */
    public static JsonObject of(
            final String k1, final JsonValue v1,
            final String k2, final JsonValue v2,
            final String k3, final JsonValue v3) {
        return new JsonObject(
                buildKeys(k1, k2, k3),
                buildValues(v1, v2, v3));
    }

    /**
     * Returns a JSON object containing the specified four JSON key-value mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value, not null
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value, not null
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value, not null
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value, not null
     * @return a JSON object containing the specified mappings
     * @throws NullPointerException  if any key or any value is {@code null}
     *
     * @since 0.10.42
     */
    public static JsonObject of(
            final String k1, final JsonValue v1,
            final String k2, final JsonValue v2,
            final String k3, final JsonValue v3,
            final String k4, final JsonValue v4) {
        return new JsonObject(
                buildKeys(k1, k2, k3, k4),
                buildValues(v1, v2, v3, v4));
    }

    /**
     * Returns a JSON object containing the specified five JSON key-value mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value, not null
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value, not null
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value, not null
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value, not null
     * @param k5 the fifth mapping's key, not null
     * @param v5 the fifth mapping's value, not null
     * @return a JSON object containing the specified mappings
     * @throws NullPointerException  if any key or any value is {@code null}
     *
     * @since 0.10.42
     */
    public static JsonObject of(
            final String k1, final JsonValue v1,
            final String k2, final JsonValue v2,
            final String k3, final JsonValue v3,
            final String k4, final JsonValue v4,
            final String k5, final JsonValue v5) {
        return new JsonObject(
                buildKeys(k1, k2, k3, k4, k5),
                buildValues(v1, v2, v3, v4, v5));
    }

    /**
     * Returns a JSON object containing the specified six JSON key-value mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value, not null
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value, not null
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value, not null
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value, not null
     * @param k5 the fifth mapping's key, not null
     * @param v5 the fifth mapping's value, not null
     * @param k6 the sixth mapping's key, not null
     * @param v6 the sixth mapping's value, not null
     * @return a JSON object containing the specified mappings
     * @throws NullPointerException  if any key or any value is {@code null}
     *
     * @since 0.10.42
     */
    public static JsonObject of(
            final String k1, final JsonValue v1,
            final String k2, final JsonValue v2,
            final String k3, final JsonValue v3,
            final String k4, final JsonValue v4,
            final String k5, final JsonValue v5,
            final String k6, final JsonValue v6) {
        return new JsonObject(
                buildKeys(k1, k2, k3, k4, k5, k6),
                buildValues(v1, v2, v3, v4, v5, v6));
    }

    /**
     * Returns a JSON object containing the specified seven JSON key-value mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value, not null
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value, not null
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value, not null
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value, not null
     * @param k5 the fifth mapping's key, not null
     * @param v5 the fifth mapping's value, not null
     * @param k6 the sixth mapping's key, not null
     * @param v6 the sixth mapping's value, not null
     * @param k7 the seventh mapping's key, not null
     * @param v7 the seventh mapping's value, not null
     * @return a JSON object containing the specified mappings
     * @throws NullPointerException  if any key or any value is {@code null}
     *
     * @since 0.10.42
     */
    public static JsonObject of(
            final String k1, final JsonValue v1,
            final String k2, final JsonValue v2,
            final String k3, final JsonValue v3,
            final String k4, final JsonValue v4,
            final String k5, final JsonValue v5,
            final String k6, final JsonValue v6,
            final String k7, final JsonValue v7) {
        return new JsonObject(
                buildKeys(k1, k2, k3, k4, k5, k6, k7),
                buildValues(v1, v2, v3, v4, v5, v6, v7));
    }

    /**
     * Returns a JSON object containing the specified eight JSON key-value mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value, not null
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value, not null
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value, not null
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value, not null
     * @param k5 the fifth mapping's key, not null
     * @param v5 the fifth mapping's value, not null
     * @param k6 the sixth mapping's key, not null
     * @param v6 the sixth mapping's value, not null
     * @param k7 the seventh mapping's key, not null
     * @param v7 the seventh mapping's value, not null
     * @param k8 the eighth mapping's key, not null
     * @param v8 the eighth mapping's value, not null
     * @return a JSON object containing the specified mappings
     * @throws NullPointerException  if any key or any value is {@code null}
     *
     * @since 0.10.42
     */
    public static JsonObject of(
            final String k1, final JsonValue v1,
            final String k2, final JsonValue v2,
            final String k3, final JsonValue v3,
            final String k4, final JsonValue v4,
            final String k5, final JsonValue v5,
            final String k6, final JsonValue v6,
            final String k7, final JsonValue v7,
            final String k8, final JsonValue v8) {
        return new JsonObject(
                buildKeys(k1, k2, k3, k4, k5, k6, k7, k8),
                buildValues(v1, v2, v3, v4, v5, v6, v7, v8));
    }

    /**
     * Returns a JSON object containing the specified nine JSON key-value mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value, not null
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value, not null
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value, not null
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value, not null
     * @param k5 the fifth mapping's key, not null
     * @param v5 the fifth mapping's value, not null
     * @param k6 the sixth mapping's key, not null
     * @param v6 the sixth mapping's value, not null
     * @param k7 the seventh mapping's key, not null
     * @param v7 the seventh mapping's value, not null
     * @param k8 the eighth mapping's key, not null
     * @param v8 the eighth mapping's value, not null
     * @param k9 the ninth mapping's key, not null
     * @param v9 the ninth mapping's value, not null
     * @return a JSON object containing the specified mappings
     * @throws NullPointerException  if any key or any value is {@code null}
     *
     * @since 0.10.42
     */
    public static JsonObject of(
            final String k1, final JsonValue v1,
            final String k2, final JsonValue v2,
            final String k3, final JsonValue v3,
            final String k4, final JsonValue v4,
            final String k5, final JsonValue v5,
            final String k6, final JsonValue v6,
            final String k7, final JsonValue v7,
            final String k8, final JsonValue v8,
            final String k9, final JsonValue v9) {
        return new JsonObject(
                buildKeys(k1, k2, k3, k4, k5, k6, k7, k8, k9),
                buildValues(v1, v2, v3, v4, v5, v6, v7, v8, v9));
    }

    /**
     * Returns a JSON object containing the specified ten JSON key-value mappings.
     *
     * @param k1 the first mapping's key, not null
     * @param v1 the first mapping's value, not null
     * @param k2 the second mapping's key, not null
     * @param v2 the second mapping's value, not null
     * @param k3 the third mapping's key, not null
     * @param v3 the third mapping's value, not null
     * @param k4 the fourth mapping's key, not null
     * @param v4 the fourth mapping's value, not null
     * @param k5 the fifth mapping's key, not null
     * @param v5 the fifth mapping's value, not null
     * @param k6 the sixth mapping's key, not null
     * @param v6 the sixth mapping's value, not null
     * @param k7 the seventh mapping's key, not null
     * @param v7 the seventh mapping's value, not null
     * @param k8 the eighth mapping's key, not null
     * @param v8 the eighth mapping's value, not null
     * @param k9 the ninth mapping's key, not null
     * @param v9 the ninth mapping's value, not null
     * @param k10 the tenth mapping's key, not null
     * @param v10 the tenth mapping's value, not null
     * @return a JSON object containing the specified mappings
     * @throws NullPointerException  if any key or any value is {@code null}
     *
     * @since 0.10.42
     */
    public static JsonObject of(
            final String k1, final JsonValue v1,
            final String k2, final JsonValue v2,
            final String k3, final JsonValue v3,
            final String k4, final JsonValue v4,
            final String k5, final JsonValue v5,
            final String k6, final JsonValue v6,
            final String k7, final JsonValue v7,
            final String k8, final JsonValue v8,
            final String k9, final JsonValue v9,
            final String k10, final JsonValue v10) {
        return new JsonObject(
                buildKeys(k1, k2, k3, k4, k5, k6, k7, k8, k9, k10),
                buildValues(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10));
    }

    /**
     * Returns a JSON object containing an arbitrary number of JSON key-value mappings.
     *
     * <p>Keys and values are specified as varargs. Even numbers of arguments must be specified. Where <i>i</i> as an
     * odd number, the <i>i</i>-th argument represents a key that must be {@link JsonString}, and the <i>i+1</i>-th
     * argument represents a value for the key at the <i>i</i>-th argument.
     *
     * @param keyValues  the JSON keys and values to be contained in the JSON object
     * @return a JSON object containing the specified JSON key-value mappings
     * @throws IllegalArgumentException  if odd numbers of arguments are specified, or non-{@link JsonString} is specified as key
     *
     * @since 0.10.42
     */
    public static JsonObject of(final JsonValue... keyValues) {
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
        return new JsonObject(keys, values);
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
        final String[] keys = new String[entries.length];
        final JsonValue[] values = new JsonValue[entries.length];
        for (int i = 0; i < entries.length; ++i) {
            keys[i] = entries[i].getKey();
            values[i] = entries[i].getValue();
        }
        return new JsonObject(keys, values);
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
        final String[] keys = new String[entries.length];
        final JsonValue[] values = new JsonValue[entries.length];
        for (int i = 0; i < entries.length; ++i) {
            keys[i] = entries[i].getKey().getString();
            values[i] = entries[i].getValue();
        }
        return new JsonObject(keys, values);
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
        final String[] keys = new String[map.size()];
        final JsonValue[] values = new JsonValue[map.size()];
        int index = 0;
        for (final Map.Entry<String, JsonValue> pair : map.entrySet()) {
            keys[index] = pair.getKey();
            values[index] = pair.getValue();
            index++;
        }
        return new JsonObject(keys, values);
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
        final String[] keys = new String[map.size()];
        final JsonValue[] values = new JsonValue[map.size()];
        int index = 0;
        for (final Map.Entry<JsonString, JsonValue> pair : map.entrySet()) {
            keys[index] = pair.getKey().getString();
            values[index] = pair.getValue();
            index++;
        }
        return new JsonObject(keys, values);
    }

    /**
     * Returns a JSON object containing the specified arrays as its internal representation.
     *
     * <p><strong>This method is not safe.</strong> If the specified array is modified after creating a {@link JsonObject}
     * instance with this method, the created {@link JsonObject} instance can unintentionally behave differently.
     *
     * @param keys  the array of strings to be the internal representation as the keys in the new {@link JsonObject}
     * @param values  the array of JSON values to be the internal representation as the values in the new {@link JsonObject}
     * @return a JSON object containing the specified array as the internal representation
     *
     * @since 0.10.42
     */
    public static JsonObject ofUnsafe(final String[] keys, final JsonValue[] values) {
        return new JsonObject(keys, values);
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
            return JsonObject.ofMap(this.map);
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
            this.map.put(key, value);
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
            this.map.put(key.getString(), value);
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
            this.put(entry.getKey(), entry.getValue());
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
            this.put(entry.getKey().getString(), entry.getValue());
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

        private final LinkedHashMap<String, JsonValue> map;
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
     * Returns {@link JsonValue.EntityType#OBJECT}, which is the entity type of {@link JsonObject}.
     *
     * @return {@link JsonValue.EntityType#OBJECT}, which is the entity type of {@link JsonObject}
     *
     * @since 0.10.42
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.OBJECT;
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
     * Returns the approximate size of this JSON object in bytes presumed to occupy in {@link org.embulk.spi.Page} as a reference.
     *
     * <p>This approximate size is used only as a threshold whether {@link org.embulk.spi.PageBuilder} is flushed, or not.
     * It is not accurate, it does not need to be accurate, and it is impossible in general to tell an accurate size that
     * a Java object occupies in the Java heap. But, a reasonable approximate would help to keep {@link org.embulk.spi.Page}
     * performant in the Java heap.
     *
     * <p>It is better to flush more frequently for bigger JSON value objects, less often for smaller JSON value objects,
     * but no infinite accumulation even for empty JSON value objects.
     *
     * @return the approximate size of this JSON object in bytes presumed to occupy in {@link org.embulk.spi.Page} as a reference
     *
     * @see "org.embulk.spi.PageBuilderImpl#addRecord"
     */
    @Override
    public int presumeReferenceSizeInBytes() {
        // No clear reason for the number 4.
        // But at least, it should not be 0 so that the approximate size of an empty object would not be 0.
        int sum = 4;

        for (int i = 0; i < this.values.length; i++) {
            sum += this.keys[i].length() * 2 + 4;
            sum += this.values[i].presumeReferenceSizeInBytes();
        }
        return sum;
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
        return this.keys.length;
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
        return new EntrySet(this.keys, this.values);
    }

    /**
     * Returns the key-value pairs as an array of {@code JsonValue}.
     *
     * Odd elements are keys. Next element of an odd element is a value corresponding to the key.
     *
     * For example, if this value represents <code>{"k1": "v1", "k2": "v2"}</code>, this method returns ["k1", "v1", "k2", "v2"].
     */
    public JsonValue[] getKeyValueArray() {
        final JsonValue[] keyValues = new JsonValue[this.keys.length * 2];
        for (int i = 0; i < this.keys.length; i++) {
            keyValues[i * 2] = JsonString.of(this.keys[i]);
            keyValues[i * 2 + 1] = this.values[i];
        }
        return keyValues;
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

    /**
     * Returns the string representation of this JSON object.
     *
     * @return the string representation of this JSON object
     *
     * @since 0.10.42
     */
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

    /**
     * Compares the specified object with this JSON object for equality.
     *
     * <p>Note that it can return {@code true} only when {@link JsonObject} is given. It checks the equality as a JSON object.
     * It does not return {@code true} for a general {@link java.util.Map} even though the given map contains the same mapping.
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

        // Check by `instanceof` in case against unexpected arbitrary extension of JsonValue.
        if (!(otherObject instanceof JsonObject)) {
            return false;
        }

        final JsonObject other = (JsonObject) otherObject;

        // The equality of JsonObject should be checked like a Map, not by the internal key-value array.
        // For example, the order of the internal key-value array should not impact the equality of JsonObject.
        return Objects.equals(this.entrySet(), other.entrySet());
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

    private static String[] buildKeys(final String... keys) {
        for (final String key : keys) {
            if (key == null) {
                throw new NullPointerException("null in keys.");
            }
        }
        return keys;
    }

    private static JsonValue[] buildValues(final JsonValue... values) {
        for (final JsonValue value : values) {
            if (value == null) {
                throw new NullPointerException("null in values.");
            }
        }
        return values;
    }

    private static final JsonObject EMPTY = new JsonObject(new String[0], new JsonValue[0]);

    private final String[] keys;
    private final JsonValue[] values;
}
