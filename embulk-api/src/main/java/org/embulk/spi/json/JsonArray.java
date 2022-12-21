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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents an array in JSON.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format</a>
 *
 * @since 0.10.42
 */
public final class JsonArray extends AbstractList<JsonValue> implements JsonValue {
    private JsonArray(final JsonValue[] values) {
        this.values = values;
    }

    /**
     * Returns a JSON array containing zero JSON values.
     *
     * @return an empty JSON array
     *
     * @since 0.10.42
     */
    public static JsonArray of() {
        return EMPTY;
    }

    /**
     * Returns a JSON array containing an arbitrary number of JSON values.
     *
     * @param values  the JSON values to be contained in the JSON array
     * @return a JSON array containing the specified JSON values
     *
     * @since 0.10.42
     */
    public static JsonArray of(final JsonValue... values) {
        if (values.length == 0) {
            return EMPTY;
        }
        return new JsonArray(Arrays.copyOf(values, values.length));
    }

    /**
     * Returns a JSON array containing an arbitrary number of JSON values.
     *
     * @param values  the JSON values to be contained in the JSON array
     * @return a JSON array containing the specified JSON values
     *
     * @since 0.10.42
     */
    public static JsonArray ofList(final List<? extends JsonValue> values) {
        if (values.isEmpty()) {
            return EMPTY;
        }
        return new JsonArray(values.toArray(new JsonValue[values.size()]));
    }

    /**
     * Returns a JSON array containing the specified array as its internal representation.
     *
     * <p><strong>This method is not safe.</strong> If the specified array is modified after creating a {@link JsonArray}
     * instance with this method, the created {@link JsonArray} instance can unintentionally behave differently.
     *
     * @param array  the array of JSON values to be the internal representation as-is in the new {@link JsonArray}
     * @return a JSON array containing the specified array as the internal representation
     *
     * @since 0.10.42
     */
    public static JsonArray ofUnsafe(final JsonValue... array) {
        return new JsonArray(array);
    }

    /**
     * Returns {@link JsonValue.Type#ARRAY}, which is the type of {@link JsonArray}.
     *
     * @return {@link JsonValue.Type#ARRAY}, which is the type of {@link JsonArray}
     *
     * @since 0.10.42
     */
    @Override
    public Type getType() {
        return Type.ARRAY;
    }

    /**
     * Returns this value as {@link JsonArray}.
     *
     * @return itself as {@link JsonArray}
     *
     * @since 0.10.42
     */
    @Override
    public JsonArray asJsonArray() {
        return this;
    }

    /**
     * Returns the number of JSON values in this array.
     *
     * @return the number of JSON values in this array
     *
     * @since 0.10.42
     */
    @Override
    public int size() {
        return this.values.length;
    }

    /**
     * Returns the JSON value at the specified position in this array.
     *
     * @param index  index of the JSON value to return
     * @return the JSON value at the specified position in this array
     * @throws IndexOutOfBoundsException  If the index is out of range ({@code index < 0 || index >= size()})
     *
     * @since 0.10.42
     */
    @Override
    public JsonValue get(final int index) {
        return this.values[index];
    }

    /**
     * Returns the stringified JSON representation of this JSON array.
     *
     * @return the stringified JSON representation of this JSON array
     *
     * @since 0.10.42
     */
    @Override
    public String toJson() {
        if (this.values.length == 0) {
            return "[]";
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(this.values[0].toJson());
        for (int i = 1; i < this.values.length; i++) {
            builder.append(",");
            builder.append(this.values[i].toJson());
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Returns the string representation of this JSON array.
     *
     * @return the string representation of this JSON array
     *
     * @since 0.10.42
     */
    @Override
    public String toString() {
        if (this.values.length == 0) {
            return "[]";
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(this.values[0].toString());
        for (int i = 1; i < this.values.length; i++) {
            builder.append(",");
            builder.append(this.values[i].toString());
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Compares the specified object with this JSON array for equality.
     *
     * @return {@code true} if the specified object is equal to this JSON array
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

        if (otherValue instanceof JsonArray) {
            final JsonArray other = (JsonArray) otherValue;
            return Arrays.equals(this.values, other.values);
        }

        if (!otherValue.isArray()) {
            return false;
        }
        final JsonArray other = otherValue.asJsonArray();
        if (this.size() != other.size()) {
            return false;
        }

        for (int i = 0; i < this.size(); i++) {
            if (!this.get(i).equals(other.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the hash code value for this JSON array.
     *
     * @return the hash code value for this JSON array
     *
     * @since 0.10.42
     */
    @Override
    public int hashCode() {
        int hash = 1;
        for (int i = 0; i < this.values.length; i++) {
            final JsonValue value = this.values[i];
            hash = 31 * hash + value.hashCode();
        }
        return hash;
    }

    private static final JsonArray EMPTY = new JsonArray(new JsonValue[0]);

    private final JsonValue[] values;
}
