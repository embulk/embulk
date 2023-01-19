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
 * Represents {@code true} or {@code false} in JSON.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format</a>
 *
 * @since 0.10.42
 */
public final class JsonBoolean implements JsonValue {
    private JsonBoolean(final boolean value) {
        // No direct instantiation.
        this.value = value;
    }

    /**
     * The singleton instance that represents {@code false} in JSON.
     *
     * @since 0.10.42
     */
    public static final JsonBoolean FALSE = new JsonBoolean(false);

    /**
     * The singleton instance that represents {@code true} in JSON.
     *
     * @since 0.10.42
     */
    public static final JsonBoolean TRUE = new JsonBoolean(true);

    /**
     * Returns the singleton instance of {@link JsonBoolean#TRUE} or {@link JsonBoolean#FALSE}.
     *
     * @return the singleton instance of {@link JsonBoolean#TRUE} or {@link JsonBoolean#FALSE}
     *
     * @since 0.10.42
     */
    public static JsonBoolean of(final boolean value) {
        if (value) {
            return TRUE;
        }
        return FALSE;
    }

    /**
     * Returns {@link JsonValue.EntityType#BOOLEAN}, which is the entity type of {@link JsonBoolean}.
     *
     * @return {@link JsonValue.EntityType#BOOLEAN}, which is the entity type of {@link JsonBoolean}
     *
     * @since 0.10.42
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.BOOLEAN;
    }

    /**
     * Returns this value as {@link JsonBoolean}.
     *
     * @return itself as {@link JsonBoolean}
     *
     * @since 0.10.42
     */
    @Override
    public JsonBoolean asJsonBoolean() {
        return this;
    }

    /**
     * Returns this JSON {@code true} or {@code false} as a primitive {@code boolean}.
     *
     * @return the {@code boolean} representation of this JSON {@code true} or {@code false}
     *
     * @since 0.10.42
     */
    public boolean booleanValue() {
        return this.value;
    }

    /**
     * Returns the stringified JSON representation of this JSON {@code true} or {@code false}.
     *
     * @return the stringified JSON representation of this JSON {@code true} or {@code false}
     *
     * @since 0.10.42
     */
    @Override
    public String toJson() {
        if (this.value) {
            return "true";
        }
        return "false";
    }

    /**
     * Returns the string representation of this JSON {@code true} or {@code false}.
     *
     * @return the string representation of this JSON {@code true} or {@code false}
     *
     * @since 0.10.42
     */
    @Override
    public String toString() {
        if (this.value) {
            return "true";
        }
        return "false";
    }

    /**
     * Compares the specified object with this JSON {@code true} or {@code false} for equality.
     *
     * @return {@code true} if the specified object is equal to this JSON {@code true} or {@code false}
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

        final JsonValue other = (JsonValue) otherObject;
        if (!other.isJsonBoolean()) {
            return false;
        }
        return value == other.asJsonBoolean().booleanValue();
    }

    /**
     * Returns the hash code value for this JSON {@code true} or {@code false}.
     *
     * @return the hash code value for this JSON {@code true} or {@code false}
     *
     * @since 0.10.42
     */
    @Override
    public int hashCode() {
        if (this.value) {
            return 1231;
        }
        return 1237;
    }

    private final boolean value;
}
