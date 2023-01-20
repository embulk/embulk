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
 * Represents a value in JSON: {@code null}, {@code true}, {@code false}, numbers, strings, arrays, or objects.
 *
 * <p>Every implementation class of this {@code interface} should be {@code final}. It means that developers must not
 * extend the existing {@link JsonValue} implementation classes by themselves.
 *
 * <p>Each implementation class of this {@code interface} should correspond to each {@link EntityType} constant by 1:1.
 * For example, only {@link JsonLong} should be corresponding to {@link EntityType#LONG}, only {@link JsonDecimal}
 * should be corresponding to {@link EntityType#DECIMAL}, and only {@link JsonObject} should be corresponding to
 * {@link EntityType#OBJECT}.
 *
 * <p>On the other hand, developers should keep it in mind that the future Embulk may have some more {@link JsonValue}
 * implementation classes. For example, another implementation of integers backed by {@link java.math.BigInteger}, and
 * another implementation of decimals backed by {@link java.math.BigDecimal} are under consideration. When it happens,
 * new implementation classes like {@code JsonBigInteger} and {@code JsonBigDecimal}, and new entity types like
 * {@code EntityType#BIG_INTEGER} and {@code EntityType#BIG_DECIMAL} would be added.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format</a>
 *
 * @since 0.10.42
 */
public interface JsonValue {
    /**
     * A type of a JSON value entity, which should correspond to the implementation class of the JSON value instance.
     *
     * <p>Note that the entity type may not be equivalent to the value type as JSON. For example, both {@code 42} and
     * {@code 3.141592} are typed as just "numbers" as JSON. As {@link JsonValue}, however, they are normally represented
     * by different implementation classes, and then typed as different entity types. {@code 42} is usually represented by
     * {@link JsonLong} as {@code JsonLong.of(42)}, then typed as {@link EntityType#LONG}. {@code 3.141592} is
     * represented by {@link JsonDecimal} as {@code JsonDecimal.of(3.141592)}, then typed as {@link EntityType#DECIMAL}.
     *
     * @since 0.10.42
     */
    public static enum EntityType {
        /**
         * The singleton instance of the entity type for {@code null} in JSON, which is represented by {@link JsonNull}.
         *
         * @since 0.10.42
         */
        NULL,

        /**
         * The singleton instance of the entity type for {@code true} or {@code false} in JSON, which is represented by {@link JsonBoolean}.
         *
         * @since 0.10.42
         */
        BOOLEAN,

        /**
         * The singleton instance of the entity type for integral numbers in JSON, which is represented by {@link JsonLong}.
         *
         * @since 0.10.42
         */
        LONG,

        /**
         * The singleton instance of the entity type for decimal numbers in JSON, which is represented by {@link JsonDecimal}.
         *
         * @since 0.10.42
         */
        DECIMAL,

        /**
         * The singleton instance of the entity type for strings in JSON, which is represented by {@link JsonString}.
         *
         * @since 0.10.42
         */
        STRING,

        /**
         * The singleton instance of the entity type for arrays in JSON, which is represented by {@link JsonArray}.
         *
         * @since 0.10.42
         */
        ARRAY,

        /**
         * The singleton instance of the entity type for objects in JSON, which is represented by {@link JsonObject}.
         *
         * @since 0.10.42
         */
        OBJECT,
        ;

        /**
         * Returns {@code true} if the JSON value is {@code null}, which is represented by {@link JsonNull}.
         *
         * @since 0.10.42
         */
        public boolean isNull() {
            return this == NULL;
        }

        /**
         * Returns {@code true} if the JSON value is {@code true} or {@code false}, which is represented by {@link JsonBoolean}.
         *
         * @since 0.10.42
         */
        public boolean isBoolean() {
            return this == BOOLEAN;
        }

        /**
         * Returns {@code true} if the JSON value is an integral number, which is represented by {@link JsonLong}.
         *
         * @since 0.10.42
         */
        public boolean isLong() {
            return this == LONG;
        }

        /**
         * Returns {@code true} if the JSON value is a number, which is represented by {@link JsonDecimal}.
         *
         * @since 0.10.42
         */
        public boolean isDecimal() {
            return this == DECIMAL;
        }

        /**
         * Returns {@code true} if the JSON value is a string, which is represented by {@link JsonString}.
         *
         * @since 0.10.42
         */
        public boolean isString() {
            return this == STRING;
        }

        /**
         * Returns {@code true} if the JSON value is an array, which is represented by {@link JsonArray}.
         *
         * @since 0.10.42
         */
        public boolean isArray() {
            return this == ARRAY;
        }

        /**
         * Returns {@code true} if the JSON value is an object, which is represented by {@link JsonObject}.
         *
         * @since 0.10.42
         */
        public boolean isObject() {
            return this == OBJECT;
        }
    }

    /**
     * Returns the entity type of this JSON value.
     *
     * @return the entity type of this JSON value
     *
     * @since 0.10.42
     */
    EntityType getEntityType();

    /**
     * Returns {@code true} if this JSON value is {@code null}, which is {@link JsonNull}.
     *
     * <p>If this method returns {@code true}, {@link #asJsonNull} never throws exceptions.
     *
     * @return {@code true} if this JSON value is {@code null}, which is {@link JsonNull}
     *
     * @since 0.10.42
     */
    default boolean isJsonNull() {
        return this.getEntityType().isNull();
    }

    /**
     * Returns {@code true} if this JSON value is {@code true} or {@code false}, which is {@link JsonBoolean}.
     *
     * <p>If this method returns {@code true}, {@link #asJsonBoolean} never throws exceptions.
     *
     * @return {@code true} if this JSON value is {@code true} or {@code false}, which is {@link JsonBoolean}
     *
     * @since 0.10.42
     */
    default boolean isJsonBoolean() {
        return this.getEntityType().isBoolean();
    }

    /**
     * Returns {@code true} if this JSON value is an integral number, which is {@link JsonLong}.
     *
     * <p>If this method returns {@code true}, {@link #asJsonLong} never throws exceptions.
     *
     * @return {@code true} if this JSON value is an integral number, which is {@link JsonLong}
     *
     * @since 0.10.42
     */
    default boolean isJsonLong() {
        return this.getEntityType().isLong();
    }

    /**
     * Returns {@code true} if this JSON value is a number, which is {@link JsonDecimal}.
     *
     * <p>If this method returns {@code true}, {@link #asJsonDecimal} never throws exceptions.
     *
     * @return {@code true} if this JSON value is a number, which is {@link JsonDecimal}
     *
     * @since 0.10.42
     */
    default boolean isJsonDecimal() {
        return this.getEntityType().isDecimal();
    }

    /**
     * Returns {@code true} if this JSON value is a string, which is {@link JsonString}.
     *
     * If this method returns {@code true}, {@link #asJsonString} never throws exceptions.
     *
     * @return {@code true} if this JSON value is a string, which is {@link JsonString}
     *
     * @since 0.10.42
     */
    default boolean isJsonString() {
        return this.getEntityType().isString();
    }

    /**
     * Returns {@code true} if this JSON value is an array, which is {@link JsonArray}.
     *
     * <p>If this method returns {@code true}, {@link #asJsonArray} never throws exceptions.
     *
     * @return {@code true} if this JSON value is an array, which is {@link JsonArray}
     *
     * @since 0.10.42
     */
    default boolean isJsonArray() {
        return this.getEntityType().isArray();
    }

    /**
     * Returns {@code true} if this JSON value is an object, which is {@link JsonObject}.
     *
     * <p>If this method returns {@code true}, {@link #asJsonObject} never throws exceptions.
     *
     * @return {@code true} if this JSON value is an object, which is {@link JsonObject}
     *
     * @since 0.10.42
     */
    default boolean isJsonObject() {
        return this.getEntityType().isObject();
    }

    /**
     * Returns this value as {@link JsonNull}, or throws {@link ClassCastException} otherwise.
     *
     * @return itself as {@link JsonNull}
     * @throws ClassCastException  if this JSON value is not {@code null}, not {@link JsonNull}
     *
     * @since 0.10.42
     */
    default JsonNull asJsonNull() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonNull.");
    }

    /**
     * Returns this value as {@link JsonBoolean}, or throws {@link ClassCastException} otherwise.
     *
     * @return itself as {@link JsonBoolean}
     * @throws ClassCastException  if this JSON value is not {@code true} nor {@code false}, not {@link JsonBoolean}
     *
     * @since 0.10.42
     */
    default JsonBoolean asJsonBoolean() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonBoolean.");
    }

    /**
     * Returns this value as {@link JsonLong}, or throws {@link ClassCastException} otherwise.
     *
     * @return itself as {@link JsonLong}
     * @throws ClassCastException  if this JSON value is not an integral number, not {@link JsonLong}
     *
     * @since 0.10.42
     */
    default JsonLong asJsonLong() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonLong.");
    }

    /**
     * Returns this value as {@link JsonDecimal}, or throws {@link ClassCastException} otherwise.
     *
     * @return itself as {@link JsonDecimal}
     * @throws ClassCastException  if this JSON value is not a number, not {@link JsonDecimal}
     *
     * @since 0.10.42
     */
    default JsonDecimal asJsonDecimal() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonDecimal.");
    }

    /**
     * Returns this value as {@link JsonString}, or throws {@link ClassCastException} otherwise.
     *
     * @return itself as {@link JsonString}
     * @throws ClassCastException  if this JSON value is not a string, not {@link JsonString}
     *
     * @since 0.10.42
     */
    default JsonString asJsonString() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonString.");
    }

    /**
     * Returns this value as {@link JsonArray}, or throws {@link ClassCastException} otherwise.
     *
     * @return itself as {@link JsonArray}
     * @throws ClassCastException  if this JSON value is not an array, not {@link JsonArray}
     *
     * @since 0.10.42
     */
    default JsonArray asJsonArray() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonArray.");
    }

    /**
     * Returns this value as {@link JsonObject}, or throws {@link ClassCastException} otherwise.
     *
     * @return itself as {@link JsonObject}
     * @throws ClassCastException  if this JSON value is not an object, not {@link JsonObject}
     *
     * @since 0.10.42
     */
    default JsonObject asJsonObject() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonObject.");
    }

    /**
     * Compares the specified object with this JSON value for equality.
     *
     * @return {@code true} if the specified object is equal to this JSON value
     *
     * @since 0.10.42
     */
    boolean equals(Object other);

    /**
     * Returns the stringified JSON representation of this JSON value.
     *
     * <p>{@code NaN} and {@code Infinity} of {@link JsonDecimal} are converted to {@code "null"}.
     *
     * @return the stringified JSON representation of this JSON value
     *
     * @since 0.10.42
     */
    String toJson();
}
