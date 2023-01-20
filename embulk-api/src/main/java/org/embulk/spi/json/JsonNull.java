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
     * The singleton instance that represents {@code null} in JSON.
     *
     * @since 0.10.42
     */
    public static final JsonNull NULL = new JsonNull();

    /**
     * Returns the singleton instance of {@link JsonNull}.
     *
     * @return the singleton instance of {@link JsonNull}
     *
     * @since 0.10.42
     */
    public static JsonNull of() {
        return NULL;
    }

    /**
     * Returns {@link JsonValue.EntityType#NULL}, which is the entity type of {@link JsonNull}.
     *
     * @return {@link JsonValue.EntityType#NULL}, which is the entity type of {@link JsonNull}
     *
     * @since 0.10.42
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.NULL;
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
     * Returns {@code 1} for the size of {@code null} in bytes presumed to occupy in {@link org.embulk.spi.Page} as a reference.
     *
     * @return {@code 1}
     *
     * @see <a href="https://github.com/airlift/slice/blob/0.9/src/main/java/io/airlift/slice/SizeOf.java#L37">SIZE_OF_BYTE in Airlift's Slice</a>
     */
    @Override
    public int presumeReferenceSizeInBytes() {
        // Indeed, null is stored in Page as a special form as |nullBitSet|, but considered as 1 here in JsonValue just for ease.
        return 1;
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
        // Only the singleton instance is accepted. No arbitrary instantiation.
        return this == NULL && otherObject == NULL;
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
}
