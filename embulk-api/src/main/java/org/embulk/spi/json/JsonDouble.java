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
 * Represents a number in JSON, represented by a Java primitive {@code double}, which is the same as Embulk's {@code DOUBLE} column type.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format</a>
 *
 * @since 0.10.42
 */
public final class JsonDouble implements JsonValue {
    private JsonDouble(final double value, final String literal) {
        this.value = value;
        this.literal = literal;
    }

    /**
     * Returns a JSON number represented by the specified Java primitive {@code double}.
     *
     * @param value  the number
     * @return a JSON number represented by the specified Java primitive {@code double}
     *
     * @since 0.10.42
     */
    public static JsonDouble of(final double value) {
        return new JsonDouble(value, null);
    }

    /**
     * Returns a JSON number that is represented by the specified Java primitive {@code double}, with the specified JSON literal.
     *
     * <p>The literal is just subsidiary information used when stringifying this JSON number as JSON by {@link #toJson()}.
     *
     * @param value  the number
     * @param literal  the JSON literal of the number
     * @return a JSON number represented by the specified Java primitive {@code double}
     *
     * @since 0.10.42
     */
    public static JsonDouble withLiteral(final double value, final String literal) {
        return new JsonDouble(value, literal);
    }

    /**
     * Returns {@link JsonValue.EntityType#DOUBLE}, which is the entity type of {@link JsonDouble}.
     *
     * @return {@link JsonValue.EntityType#DOUBLE}, which is the entity type of {@link JsonDouble}
     *
     * @since 0.10.42
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.DOUBLE;
    }

    /**
     * Returns this value as {@link JsonDouble}.
     *
     * @return itself as {@link JsonDouble}
     *
     * @since 0.10.42
     */
    @Override
    public JsonDouble asJsonDouble() {
        return this;
    }

    /**
     * Returns {@code true} if this JSON number is integral.
     *
     * <p>Note that it does not guarantee this JSON number can be represented as a Java primitive exact {@code long}.
     * This JSON number can be out of the range of the Java primitive {@code long}.
     *
     * @return {@code true} if this JSON number is integral
     *
     * @since 0.10.42
     */
    public boolean isIntegral() {
        return !Double.isNaN(this.value) && !Double.isInfinite(this.value) && this.value == Math.rint(this.value);
    }

    /**
     * Returns {@code true} if the JSON number is integral in the range of {@code byte}, [-2<sup>7</sup> to 2<sup>7</sup>-1].
     *
     * @return {@code true} if the JSON number is integral in the range of {@code byte}
     *
     * @since 0.10.42
     */
    public boolean isByteValue() {
        return this.isIntegral() && ((double) Byte.MIN_VALUE) <= this.value && this.value <= ((double) Byte.MAX_VALUE);
    }

    /**
     * Returns {@code true} if the JSON number is integral in the range of {@code short}, [-2<sup>15</sup> to 2<sup>15</sup>-1].
     *
     * @return {@code true} if the JSON number is integral in the range of {@code short}
     *
     * @since 0.10.42
     */
    public boolean isShortValue() {
        return this.isIntegral() && ((double) Short.MIN_VALUE) <= this.value && this.value <= ((double) Short.MAX_VALUE);
    }

    /**
     * Returns {@code true} if the JSON number is integral in the range of {@code int}, [-2<sup>31</sup> to 2<sup>31</sup>-1].
     *
     * @return {@code true} if the JSON number is integral in the range of {@code int}
     *
     * @since 0.10.42
     */
    public boolean isIntValue() {
        return this.isIntegral() && ((double) Integer.MIN_VALUE) <= this.value && this.value <= ((double) Integer.MAX_VALUE);
    }

    /**
     * Returns {@code true} if the JSON number is integral in the range of {@code long}, [-2<sup>63</sup> to 2<sup>63</sup>-1].
     *
     * @return {@code true} if the JSON number is integral in the range of {@code long}
     *
     * @since 0.10.42
     */
    public boolean isLongValue() {
        return this.isIntegral() && ((double) Long.MIN_VALUE) <= this.value && this.value <= ((double) Long.MAX_VALUE);
    }

    /**
     * Returns this JSON number as a Java primitive {@code byte}.
     *
     * <p>It narrows down {@code double} to {@code byte} as a Java primitive. Note that this conversion can lose information
     * about the magnitude of this JSON number, precision, and range.
     *
     * @return the {@code byte} representation of this JSON number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Narrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    public byte byteValue() {
        return (byte) this.value;
    }

    /**
     * Returns this JSON number as a Java primitive {@code byte}.
     *
     * <p>It throws {@link ArithmeticException} if the JSON number is out of the range of {@code byte}, or has a
     * non-zero fractional part.
     *
     * @return the {@code byte} representation of this JSON number
     * @throws ArithmeticException  if the JSON number is out of the range of {@code byte}, or has a non-zero fractional part
     *
     * @since 0.10.42
     */
    public byte byteValueExact() {
        if (!this.isIntegral()) {
            throw new ArithmeticException("Not an integer: " + this.value);
        }
        if (((double) Byte.MIN_VALUE) <= this.value && this.value <= ((double) Byte.MAX_VALUE)) {
            throw new ArithmeticException("Out of the range of byte: " + this.value);
        }
        return (byte) this.value;
    }

    /**
     * Returns this JSON number as a Java primitive {@code short}.
     *
     * <p>It narrows down {@code double} to {@code short} as a Java primitive. Note that this conversion can lose information
     * about the magnitude of this JSON number, precision, and range.
     *
     * @return the {@code short} representation of this JSON number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Narrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    public short shortValue() {
        return (short) this.value;
    }

    /**
     * Returns this JSON number as a Java primitive {@code short}.
     *
     * <p>It throws {@link ArithmeticException} if the JSON number is out of the range of {@code short}, or has a
     * non-zero fractional part.
     *
     * @return the {@code short} representation of this JSON number
     * @throws ArithmeticException  if the JSON number is out of the range of {@code short}, or has a non-zero fractional part
     *
     * @since 0.10.42
     */
    public short shortValueExact() {
        if (!this.isIntegral()) {
            throw new ArithmeticException("Not an integer: " + this.value);
        }
        if (((double) Short.MIN_VALUE) <= this.value && this.value <= ((double) Short.MAX_VALUE)) {
            throw new ArithmeticException("Out of the range of short: " + this.value);
        }
        return (short) this.value;
    }

    /**
     * Returns this JSON number as a Java primitive {@code int}.
     *
     * <p>It narrows down {@code double} to {@code int} as a Java primitive. Note that this conversion can lose information
     * about the magnitude of this JSON number, precision, and range.
     *
     * @return the {@code int} representation of this JSON number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Narrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    public int intValue() {
        return (int) this.value;
    }

    /**
     * Returns this JSON number as a Java primitive {@code int}.
     *
     * <p>It throws {@link ArithmeticException} if the JSON number is out of the range of {@code int}, or has a
     * non-zero fractional part.
     *
     * @return the {@code int} representation of this JSON number
     * @throws ArithmeticException  if the JSON number is out of the range of {@code int}, or has a non-zero fractional part
     *
     * @since 0.10.42
     */
    public int intValueExact() {
        if (!this.isIntegral()) {
            throw new ArithmeticException("Not an integer: " + this.value);
        }
        if (((double) Integer.MIN_VALUE) <= this.value && this.value <= ((double) Integer.MAX_VALUE)) {
            throw new ArithmeticException("Out of the range of int: " + this.value);
        }
        return (int) this.value;
    }

    /**
     * Returns this JSON number as a Java primitive {@code long}.
     *
     * <p>It narrows down {@code double} to {@code long} as a Java primitive. Note that this conversion can lose information
     * about the magnitude of this JSON number, precision, and range.
     *
     * @return the {@code long} representation of this JSON number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Narrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    public long longValue() {
        return (long) this.value;
    }

    /**
     * Returns this JSON number as a Java primitive {@code long}.
     *
     * <p>It throws {@link ArithmeticException} if the JSON number is out of the range of {@code long}, or has a
     * non-zero fractional part.
     *
     * @return the {@code long} representation of this JSON number
     * @throws ArithmeticException  if the JSON number is out of the range of {@code long}, or has a non-zero fractional part
     *
     * @since 0.10.42
     */
    public long longValueExact() {
        if (!this.isIntegral()) {
            throw new ArithmeticException("Not an integer: " + this.value);
        }
        if (((double) Long.MIN_VALUE) <= this.value && this.value <= ((double) Long.MAX_VALUE)) {
            throw new ArithmeticException("Out of the range of long: " + this.value);
        }
        return (long) this.value;
    }

    /**
     * Returns this JSON number as a {@link java.math.BigInteger}.
     *
     * <p>Note that this conversion loses the fractional part and the precision of the number.
     * This is a convenience method for {@code bigDecimalValue().toBigInteger()}.
     *
     * @return the {@link java.math.BigInteger} representation of this JSON number
     *
     * @since 0.10.42
     */
    public BigInteger bigIntegerValue() {
        return BigDecimal.valueOf(this.value).toBigInteger();
    }

    /**
     * Returns this JSON number as a {@link java.math.BigInteger}.
     *
     * <p>It throws {@link ArithmeticException} if the JSON number has a non-zero fractional part.
     *
     * @return the {@link java.math.BigInteger} representation of this JSON number
     * @throws ArithmeticException  if the JSON number has a non-zero fractional part
     *
     * @since 0.10.42
     */
    public BigInteger bigIntegerValueExact() {
        return BigDecimal.valueOf(this.value).toBigIntegerExact();
    }

    /**
     * Returns this JSON number as a Java primitive {@code float}.
     *
     * <p>It narrows down {@code double} to {@code float} as a Java primitive. Note that this conversion can lose information
     * about the magnitude and the precision of the number.
     *
     * @return the {@code float} representation of this JSON number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Na
rrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    public float floatValue() {
        return (float) this.value;
    }

    /**
     * Returns this JSON number as a Java primitive {@code double}, as-is.
     *
     * <p>This method does not lose any information because {@link JsonDouble} represents the number as a Java primitive
     * {@code double} inside.
     *
     * @return the {@code double} representation of this JSON number
     */
    public double doubleValue() {
        return this.value;
    }

    /**
     * Returns this JSON number as {@link java.math.BigDecimal}.
     *
     * @return the {@link java.math.BigDecimal} representation of this JSON number
     */
    public BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf(this.value);
    }

    /**
     * Returns the stringified JSON representation of this JSON number.
     *
     * <p>If this JSON number is {@code NaN} or {@code Infinity}, it returns {@code "null"}.
     *
     * <p>If this JSON number is created with a literal by {@link #withLiteral(double, String)}, it returns the literal.
     *
     * @return the stringified JSON representation of this JSON number
     *
     * @since 0.10.42
     */
    @Override
    public String toJson() {
        if (Double.isNaN(this.value) || Double.isInfinite(this.value)) {
            return "null";
        }
        if (this.literal != null) {
            return this.literal;
        }
        return Double.toString(this.value);
    }

    /**
     * Returns the string representation of this JSON number.
     *
     * @return the string representation of this JSON number
     *
     * @since 0.10.42
     */
    @Override
    public String toString() {
        return Double.toString(this.value);
    }

    /**
     * Compares the specified object with this JSON number for equality.
     *
     * @return {@code true} if the specified object is equal to this JSON number
     *
     * @since 0.10.42
     */
    @Override
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }

        // Check by `instanceof` in case against unexpected arbitrary extension of JsonValue.
        if (!(otherObject instanceof JsonDouble)) {
            return false;
        }

        final JsonDouble other = (JsonDouble) otherObject;

        return this.value == other.value;
    }

    /**
     * Returns the hash code value for this JSON number.
     *
     * @return the hash code value for this JSON number
     *
     * @since 0.10.42
     */
    @Override
    public int hashCode() {
        final long bits = Double.doubleToLongBits(this.value);
        return (int) (bits ^ (bits >>> 32));
    }

    private final double value;

    private final String literal;
}
