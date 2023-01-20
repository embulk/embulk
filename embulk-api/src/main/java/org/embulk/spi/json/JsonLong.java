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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Represents a integral number in JSON, represented by a Java primitive {@code long}, which is the same as Embulk's {@code LONG} column type.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format</a>
 *
 * @since 0.10.42
 */
public final class JsonLong implements JsonValue {
    private JsonLong(final long value, final String literal) {
        // No direct instantiation.
        this.value = value;
        this.literal = literal;
    }

    /**
     * Returns a JSON integral number represented by the specified Java primitive {@code long}.
     *
     * @param value  the integral number
     * @return a JSON integral number represented by the specified Java primitive {@code long}
     *
     * @since 0.10.42
     */
    public static JsonLong of(final long value) {
        return new JsonLong(value, null);
    }

    /**
     * Returns a JSON integral number represented by the specified Java primitive {@code long}, with the specified JSON literal.
     *
     * <p>The literal is just subsidiary information used when stringifying this integral number as JSON by {@link #toJson()}.
     * The literal does not impact the equality definition by {@link #equals(Object)}.
     *
     * @param value  the integral number
     * @param literal  the JSON literal of the integral number
     * @return a JSON integral number represented by the specified Java primitive {@code long}
     *
     * @since 0.10.42
     */
    public static JsonLong withLiteral(final long value, final String literal) {
        return new JsonLong(value, literal);
    }

    /**
     * Returns {@link JsonValue.EntityType#LONG}, which is the entity type of {@link JsonLong}.
     *
     * @return {@link JsonValue.EntityType#LONG}, which is the entity type of {@link JsonLong}
     *
     * @since 0.10.42
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.LONG;
    }

    /**
     * Returns this value as {@link JsonLong}.
     *
     * @return itself as {@link JsonLong}
     *
     * @since 0.10.42
     */
    @Override
    public JsonLong asJsonLong() {
        return this;
    }

    /**
     * Returns {@code true} to represent this JSON number is integral.
     *
     * @return {@code true}
     *
     * @since 0.10.42
     */
    public boolean isIntegral() {
        return true;
    }

    /**
     * Returns {@code true} if the JSON integral number is in the range of {@code byte}, [-2<sup>7</sup> to 2<sup>7</sup>-1].
     *
     * @return {@code true} if the JSON integral number is in the range of {@code byte}
     *
     * @since 0.10.42
     */
    public boolean isByteValue() {
        return ((long) Byte.MIN_VALUE) <= this.value && this.value <= ((long) Byte.MAX_VALUE);
    }

    /**
     * Returns {@code true} if the JSON integral number is in the range of {@code short}, [-2<sup>15</sup> to 2<sup>15</sup>-1].
     *
     * @return {@code true} if the JSON integral number is in the range of {@code short}
     *
     * @since 0.10.42
     */
    public boolean isShortValue() {
        return ((long) Short.MIN_VALUE) <= this.value && this.value <= ((long) Short.MAX_VALUE);
    }

    /**
     * Returns {@code true} if the JSON integral number is in the range of {@code int}, [-2<sup>31</sup> to 2<sup>31</sup>-1].
     *
     * @return {@code true} if the JSON integral number is in the range of {@code int}
     *
     * @since 0.10.42
     */
    public boolean isIntValue() {
        return ((long) Integer.MIN_VALUE) <= this.value && this.value <= ((long) Integer.MAX_VALUE);
    }

    /**
     * Returns {@code true}.
     *
     * @return {@code true}, always
     *
     * @since 0.10.42
     */
    public boolean isLongValue() {
        return true;
    }

    /**
     * Returns this JSON integral number as a Java primitive {@code byte}.
     *
     * <p>It narrows down {@code long} to {@code byte} as a Java primitive. Note that this conversion can lose information
     * about the magnitude of this JSON integral number, and can cause the sign of the resulting value to differ from the sign
     * of this JSON integral number.
     *
     * @return the {@code byte} representation of this JSON integral number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Narrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    public byte byteValue() {
        return (byte) this.value;
    }

    /**
     * Returns this JSON integral number as a Java primitive {@code byte}.
     *
     * <p>It throws {@link ArithmeticException} if the JSON integral number is out of the range of {@code byte}.
     *
     * @return the {@code byte} representation of this JSON integral number
     * @throws ArithmeticException  if the JSON integral number does not fit in a Java primitive {@code byte}
     *
     * @since 0.10.42
     */
    public byte byteValueExact() {
        if (!this.isByteValue()) {
            throw new ArithmeticException("Out of the range of byte: " + this.value);
        }
        return (byte) this.value;
    }

    /**
     * Returns this JSON integral number as a Java primitive {@code short}.
     *
     * <p>It narrows down {@code long} to {@code short} as a Java primitive. Note that this conversion can lose information
     * about the magnitude of this JSON integral number, and can cause the sign of the resulting value to differ from the sign
     * of this JSON integral number.
     *
     * @return the {@code short} representation of this JSON integral number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Narrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    public short shortValue() {
        return (short) this.value;
    }

    /**
     * Returns this JSON integral number as a Java primitive {@code short}.
     *
     * <p>It throws {@link ArithmeticException} if the JSON integral number is out of the range of {@code short}.
     *
     * @return the {@code short} representation of this JSON integral number
     * @throws ArithmeticException  if the JSON integral number does not fit in a Java primitive {@code short}
     *
     * @since 0.10.42
     */
    public short shortValueExact() {
        if (!this.isShortValue()) {
            throw new ArithmeticException("Out of the range of short: " + this.value);
        }
        return (short) this.value;
    }

    /**
     * Returns this JSON integral number as a Java primitive {@code int}.
     *
     * <p>It narrows down {@code long} to {@code int} as a Java primitive. Note that this conversion can lose information
     * about the magnitude of this JSON integral number, and can cause the sign of the resulting value to differ from the sign
     * of this JSON integral number.
     *
     * @return the {@code int} representation of this JSON integral number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Narrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    public int intValue() {
        return (int) this.value;
    }

    /**
     * Returns this JSON integral number as a Java primitive {@code int}.
     *
     * <p>It throws {@link ArithmeticException} if the JSON integral number is out of the range of {@code int}.
     *
     * @return the {@code int} representation of this JSON integral number
     * @throws ArithmeticException  if the JSON integral number does not fit in a Java primitive {@code int}
     *
     * @since 0.10.42
     */
    public int intValueExact() {
        if (!this.isIntValue()) {
            throw new ArithmeticException("Out of the range of int: " + this.value);
        }
        return (int) this.value;
    }

    /**
     * Returns this JSON integral number as a Java primitive {@code long}.
     *
     * @return the {@code long} representation of this JSON integral number
     *
     * @since 0.10.42
     */
    public long longValue() {
        return (long) this.value;
    }

    /**
     * Returns this JSON integral number as a Java primitive {@code long}.
     *
     * @return the {@code long} representation of this JSON integral number
     *
     * @since 0.10.42
     */
    public long longValueExact() {
        return (long) this.value;
    }

    /**
     * Returns this JSON integral number as a {@link java.math.BigInteger}.
     *
     * @return the {@link java.math.BigInteger} representation of this JSON integral number
     *
     * @since 0.10.42
     */
    public BigInteger bigIntegerValue() {
        return BigInteger.valueOf(this.value);
    }

    /**
     * Returns this JSON integral number as a {@link java.math.BigInteger}.
     *
     * @return the {@link java.math.BigInteger} representation of this JSON integral number
     *
     * @since 0.10.42
     */
    public BigInteger bigIntegerValueExact() {
        return BigInteger.valueOf(this.value);
    }

    /**
     * Returns this JSON integral number as a Java primitive {@code float}.
     *
     * <p>It widens {@code long} to {@code float} as a Java primitive. Note that this conversion can lose precision of
     * this JSON integral number.
     *
     * @return the {@code float} representation of this JSON integral number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.2">Java Language Specification - 5.1.2. Widening Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    public float floatValue() {
        return (float) this.value;
    }

    /**
     * Returns this JSON integral number as a Java primitive {@code double}.
     *
     * <p>It widens {@code long} to {@code double} as a Java primitive. Note that this conversion can lose precision of
     * this JSON integral number.
     *
     * @return the {@code double} representation of this JSON integral number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.2">Java Language Specification - 5.1.2. Widening Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    public double doubleValue() {
        return (double) this.value;
    }

    /**
     * Returns this JSON integral number as a {@link java.math.BigDecimal}.
     *
     * @return the {@link java.math.BigDecimal} representation of this JSON integral number
     *
     * @since 0.10.42
     */
    public BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf(this.value);
    }

    /**
     * Returns the stringified JSON representation of this JSON integral number.
     *
     * <p>If this JSON integral number is created with a literal by {@link #withLiteral(long, String)}, it returns the literal.
     *
     * @return the stringified JSON representation of this JSON integral number
     *
     * @since 0.10.42
     */
    @Override
    public String toJson() {
        if (this.literal != null) {
            return this.literal;
        }
        return Long.toString(this.value);
    }

    /**
     * Returns the string representation of this JSON integral number.
     *
     * @return the string representation of this JSON integral number
     *
     * @since 0.10.42
     */
    @Override
    public String toString() {
        return Long.toString(this.value);
    }

    /**
     * Compares the specified object with this JSON integral number for equality.
     *
     * @return {@code true} if the specified object is equal to this JSON integral number
     *
     * @since 0.10.42
     */
    @Override
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }

        // Check by `instanceof` in case against unexpected arbitrary extension of JsonValue.
        if (!(otherObject instanceof JsonLong)) {
            return false;
        }

        final JsonLong other = (JsonLong) otherObject;

        return this.value == other.value;
    }

    /**
     * Returns the hash code value for this JSON integral number.
     *
     * @return the hash code value for this JSON integral number
     *
     * @since 0.10.42
     */
    @Override
    public int hashCode() {
        if (((long) Integer.MIN_VALUE) <= this.value && this.value <= ((long) Integer.MAX_VALUE)) {
            return (int) value;
        }
        return (int) (value ^ (value >>> 32));
    }

    private final long value;

    private final String literal;
}
