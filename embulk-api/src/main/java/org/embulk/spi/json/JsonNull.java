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

/**
 * Represents {@code null} in JSON.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format</a>
 *
 * @since 0.10.42
 */
public final class JsonNull implements JsonValue {
    private JsonNull() {
        // No direct instantiation.
    }

    /**
     * Returns the singleton instance of {@link JsonNull}.
     *
     * @return the singleton instance of {@link JsonNull}
     *
     * @since 0.10.42
     */
    public static JsonNull of() {
        return INSTANCE;
    }

    /**
     * Returns {@link JsonValue.Type#NULL}, which is the type of {@link JsonNull}.
     *
     * @return {@link JsonValue.Type#NULL}, which is the type of {@link JsonNull}
     *
     * @since 0.10.42
     */
    @Override
    public Type getType() {
        return Type.NULL;
    }

    /**
     * Returns this value as {@link JsonNull}.
     *
     * @return itself as {@link JsonNull}
     *
     * @since 0.10.42
     */
    @Override
    public JsonNull asJsonNull() {
        return this;
    }

    /**
     * Returns the stringified JSON representation of this JSON {@code null}.
     *
     * @return {@code "null"}
     *
     * @since 0.10.42
     */
    @Override
    public String toJson() {
        return "null";
    }

    /**
     * Returns the string representation of this JSON {@code null}.
     *
     * @return {@code "null"}
     *
     * @since 0.10.42
     */
    @Override
    public String toString() {
        return "null";
    }

    /**
     * Compares the specified object with this JSON {@code null} for equality.
     *
     * @return {@code true} if the specified object is equal to this JSON {@code null}.
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
        return ((JsonValue) otherObject).isNull();
    }

    /**
     * Returns the hash code value for this JSON {@code null}.
     *
     * @return the hash code value for this JSON {@code null}
     *
     * @since 0.10.42
     */
    @Override
    public int hashCode() {
        return 0;
    }

    private static final JsonNull INSTANCE = new JsonNull();
}
