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
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format</a>
 *
 * @since 0.10.42
 */
public interface JsonValue {
    /**
     * A type of a JSON value.
     *
     * @since 0.10.42
     */
    public static enum Type {
        /**
         * The singleton instance that represents the type of {@code null} in JSON.
         *
         * @since 0.10.42
         */
        NULL,

        /**
         * The singleton instance that represents the type of {@code true} or {@code false} in JSON.
         *
         * @since 0.10.42
         */
        BOOLEAN,

        /**
         * The singleton instance that represents the type of integer numbers in JSON.
         *
         * @since 0.10.42
         */
        INTEGER,

        /**
         * The singleton instance that represents the type of decimal numbers in JSON.
         *
         * @since 0.10.42
         */
        DECIMAL,

        /**
         * The singleton instance that represents the type of strings in JSON.
         *
         * @since 0.10.42
         */
        STRING,

        /**
         * The singleton instance that represents the type of arrays in JSON.
         *
         * @since 0.10.42
         */
        ARRAY,

        /**
         * The singleton instance that represents the type of objects in JSON.
         *
         * @since 0.10.42
         */
        OBJECT,
        ;

        /**
         * Returns {@code true} if the JSON value is {@code null}.
         *
         * @since 0.10.42
         */
        public boolean isNull() {
            return this == NULL;
        }

        /**
         * Returns {@code true} if the JSON value is {@code true} or {@code false}.
         *
         * @since 0.10.42
         */
        public boolean isBoolean() {
            return this == BOOLEAN;
        }

        /**
         * Returns {@code true} if the JSON value is a number represented as an integer.
         *
         * @since 0.10.42
         */
        public boolean isInteger() {
            return this == INTEGER;
        }

        /**
         * Returns {@code true} if the JSON value is a number represented as a decimal.
         *
         * @since 0.10.42
         */
        public boolean isDecimal() {
            return this == DECIMAL;
        }

        /**
         * Returns {@code true} if the JSON value is a string.
         *
         * @since 0.10.42
         */
        public boolean isString() {
            return this == STRING;
        }

        /**
         * Returns {@code true} if the JSON value is an array.
         *
         * @since 0.10.42
         */
        public boolean isArray() {
            return this == ARRAY;
        }

        /**
         * Returns {@code true} if the JSON value is an object.
         *
         * @since 0.10.42
         */
        public boolean isObject() {
            return this == OBJECT;
        }
    }

    /**
     * Returns the type of this JSON value.
     *
     * @return the type of this JSON value
     *
     * @since 0.10.42
     */
    Type getType();

    /**
     * Returns {@code true} if the type of this JSON value is {@code null}.
     *
     * <p>If this method returns {@code true}, {@link #asJsonNull} never throws exceptions.
     *
     * @return {@code true} if type of this JSON value is {@code null}
     *
     * @since 0.10.42
     */
    default boolean isNull() {
        return this.getType().isNull();
    }

    /**
     * Returns {@code true} if the type of this JSON value is {@code true} or {@code false}.
     *
     * <p>If this method returns {@code true}, {@link #asJsonBoolean} never throws exceptions.
     *
     * @return {@code true} if type of this JSON value is {@code true} or {@code false}
     *
     * @since 0.10.42
     */
    default boolean isBoolean() {
        return this.getType().isBoolean();
    }

    /**
     * Returns {@code true} if the type of this JSON value is a number that is represented as an integer.
     *
     * <p>If this method returns {@code true}, {@link #asJsonInteger} never throws exceptions.
     *
     * @return {@code true} if type of this JSON value is a number that is represented as an integer
     *
     * @since 0.10.42
     */
    default boolean isInteger() {
        return this.getType().isInteger();
    }

    /**
     * Returns {@code true} if the type of this JSON value is a number that is represented as a decimal.
     *
     * <p>If this method returns {@code true}, {@link #asJsonDecimal} never throws exceptions.
     *
     * @return {@code true} if type of this JSON value is a number that is represented as a decimal
     *
     * @since 0.10.42
     */
    default boolean isDecimal() {
        return this.getType().isDecimal();
    }

    /**
     * Returns {@code true} if the type of this JSON value is a string.
     *
     * If this method returns {@code true}, {@link #asJsonString} never throws exceptions.
     *
     * @return {@code true} if type of this JSON value is a string
     *
     * @since 0.10.42
     */
    default boolean isString() {
        return this.getType().isString();
    }

    /**
     * Returns {@code true} if the type of this JSON value is an array.
     *
     * <p>If this method returns {@code true}, {@link #asJsonArray} never throws exceptions.
     *
     * @return {@code true} if type of this JSON value is an array
     *
     * @since 0.10.42
     */
    default boolean isArray() {
        return this.getType().isArray();
    }

    /**
     * Returns {@code true} if the type of this JSON value is an object.
     *
     * <p>If this method returns {@code true}, {@link #asJsonObject} never throws exceptions.
     *
     * @return {@code true} if type of this JSON value is an object
     *
     * @since 0.10.42
     */
    default boolean isObject() {
        return this.getType().isObject();
    }

    /**
     * Returns this value as {@link JsonNull}, or throws {@code ClassCastException} otherwise.
     *
     * @return itself as {@link JsonNull}
     * @throws ClassCastException  if this JSON value is not {@code null}
     *
     * @since 0.10.42
     */
    default JsonNull asJsonNull() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonNull.");
    }

    /**
     * Returns this value as {@code JsonBoolean}, or throws {@code ClassCastException} otherwise.
     *
     * @return itself as {@link JsonBoolean}
     * @throws ClassCastException  if this JSON value is not {@code true} nor {@code false}
     *
     * @since 0.10.42
     */
    default JsonBoolean asJsonBoolean() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonBoolean.");
    }

    /**
     * Returns this value as {@code JsonInteger}, or throws {@code ClassCastException} otherwise.
     *
     * @return itself as {@link JsonInteger}
     * @throws ClassCastException  if this JSON value is not a number represented as an integer
     *
     * @since 0.10.42
     */
    default JsonInteger asJsonInteger() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonInteger.");
    }

    /**
     * Returns this value as {@code JsonDecimal}, or throws {@code ClassCastException} otherwise.
     *
     * @return itself as {@link JsonDecimal}
     * @throws ClassCastException  if this JSON value is not a number represented as a decimal
     *
     * @since 0.10.42
     */
    default JsonDecimal asJsonDecimal() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonDecimal.");
    }

    /**
     * Returns this value as {@code JsonString}, or throws {@code ClassCastException} otherwise.
     *
     * @return itself as {@link JsonString}
     * @throws ClassCastException  if this JSON value is not a number represented as a string
     *
     * @since 0.10.42
     */
    default JsonString asJsonString() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonString.");
    }

    /**
     * Returns this value as {@code JsonArray}, or throws {@code ClassCastException} otherwise.
     *
     * @return itself as {@link JsonArray}
     * @throws ClassCastException  if this JSON value is not a number represented as an array
     *
     * @since 0.10.42
     */
    default JsonArray asJsonArray() {
        throw new ClassCastException(this.getClass().getSimpleName() + " cannot be cast to JsonArray.");
    }

    /**
     * Returns this value as {@code JsonObject}, or throws {@code ClassCastException} otherwise.
     *
     * @return itself as {@link JsonObject}
     * @throws ClassCastException  if this JSON value is not a number represented as an object
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
