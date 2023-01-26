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

import org.msgpack.value.Value;
import org.msgpack.value.impl.ImmutableBooleanValueImpl;

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
     * Returns {@code 1} for the size of {@code byte} in bytes presumed to occupy in {@link org.embulk.spi.Page} as a reference.
     *
     * <p>This approximate size is used only as a threshold whether {@link org.embulk.spi.PageBuilder} is flushed, or not.
     * It is not accurate, it does not need to be accurate, and it is impossible in general to tell an accurate size that
     * a Java object occupies in the Java heap. But, a reasonable approximate would help to keep {@link org.embulk.spi.Page}
     * performant in the Java heap.
     *
     * <p>It is better to flush more frequently for bigger JSON value objects, less often for smaller JSON value objects,
     * but no infinite accumulation even for empty JSON value objects.
     *
     * @return {@code 1}
     *
     * @see "org.embulk.spi.PageBuilderImpl#addRecord"
     *
     * @see <a href="https://github.com/airlift/slice/blob/0.9/src/main/java/io/airlift/slice/SizeOf.java#L37">SIZE_OF_BYTE in Airlift's Slice</a>
     */
    @Override
    public int presumeReferenceSizeInBytes() {
        // BOOLEAN is stored in Page as a byte.
        return 1;
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
     * Returns the corresponding MessagePack's Boolean value of this JSON {@code true} or {@code false}.
     *
     * @return the corresponding MessagePack's Boolean value of this JSON {@code true} or {@code false}
     *
     * @see <a href="https://github.com/embulk/embulk/pull/1538">Draft EEP: JSON Column Type</a>
     *
     * @deprecated Do not use this method. It is to be removed at some point after Embulk v1.0.0.
     *     It is here only to ensure a migration period from MessagePack-based JSON values to new
     *     JSON values of {@link JsonValue}.
     *
     * @since 0.10.42
     */
    @Deprecated
    @Override
    public Value toMsgpack() {
        if (this.value) {
            return ImmutableBooleanValueImpl.TRUE;
        }
        return ImmutableBooleanValueImpl.FALSE;
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
        // Only the singleton instances FALSE and TRUE are accepted. No arbitrary instantiation.
        return ((this == FALSE && otherObject == FALSE) || (this == TRUE && otherObject == TRUE));
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
