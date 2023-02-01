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
import org.msgpack.value.ImmutableIntegerValue;
import org.msgpack.value.IntegerValue;
import org.msgpack.value.Value;
import org.msgpack.value.impl.ImmutableLongValueImpl;

/**
 * Represents a integral number in JSON, represented by a Java primitive {@code long}, which is the same as Embulk's {@code LONG} column type.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format</a>
 *
 * @since 0.10.42
 */
public final class JsonLong implements JsonNumber {
    private JsonLong(final long value, final String literal) {
        // No direct instantiation.
        this.value = new ImmutableLongValueImpl(value);
        this.literal = literal;
    }

    private JsonLong(final ImmutableLongValueImpl msgpackValue) {
        this.value = msgpackValue;
        this.literal = null;
    }

    static JsonLong fromMsgpack(final IntegerValue msgpackValue) {
        final ImmutableIntegerValue immutableInteger = (ImmutableIntegerValue) msgpackValue.immutableValue();
        if (immutableInteger instanceof ImmutableLongValueImpl) {
            return new JsonLong((ImmutableLongValueImpl) immutableInteger);
        }
        throw new IllegalArgumentException("MessagePack's Integer type is not long.");
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
     * Returns {@code 8} for the size of {@code long} in bytes presumed to occupy in {@link org.embulk.spi.Page} as a reference.
     *
     * <p>This approximate size is used only as a threshold whether {@link org.embulk.spi.PageBuilder} is flushed, or not.
     * It is not accurate, it does not need to be accurate, and it is impossible in general to tell an accurate size that
     * a Java object occupies in the Java heap. But, a reasonable approximate would help to keep {@link org.embulk.spi.Page}
     * performant in the Java heap.
     *
     * <p>It is better to flush more frequently for bigger JSON value objects, less often for smaller JSON value objects,
     * but no infinite accumulation even for empty JSON value objects.
     *
     * @return {@code 8}
     *
     * @see "org.embulk.spi.PageBuilderImpl#addRecord"
     *
     * @see <a href="https://github.com/airlift/slice/blob/0.9/src/main/java/io/airlift/slice/SizeOf.java#L40">SIZE_OF_LONG in Airlift's Slice</a>
     */
    @Override
    public int presumeReferenceSizeInBytes() {
        return 8;
    }

    /**
     * Returns {@code true} to represent this JSON number is integral.
     *
     * @return {@code true}
     *
     * @since 0.10.42
     */
    @Override
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
    @Override
    public boolean isByteValue() {
        return this.value.isInByteRange();
    }

    /**
     * Returns {@code true} if the JSON integral number is in the range of {@code short}, [-2<sup>15</sup> to 2<sup>15</sup>-1].
     *
     * @return {@code true} if the JSON integral number is in the range of {@code short}
     *
     * @since 0.10.42
     */
    @Override
    public boolean isShortValue() {
        return this.value.isInShortRange();
    }

    /**
     * Returns {@code true} if the JSON integral number is in the range of {@code int}, [-2<sup>31</sup> to 2<sup>31</sup>-1].
     *
     * @return {@code true} if the JSON integral number is in the range of {@code int}
     *
     * @since 0.10.42
     */
    @Override
    public boolean isIntValue() {
        return this.value.isInIntRange();
    }

    /**
     * Returns {@code true}.
     *
     * @return {@code true}, always
     *
     * @since 0.10.42
     */
    @Override
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
    @Override
    public byte byteValue() {
        return this.value.toByte();
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
    @Override
    public byte byteValueExact() {
        if (!this.isByteValue()) {
            throw new ArithmeticException("Out of the range of byte: " + this.value);
        }
        return this.value.toByte();
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
    @Override
    public short shortValue() {
        return this.value.toShort();
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
    @Override
    public short shortValueExact() {
        if (!this.isShortValue()) {
            throw new ArithmeticException("Out of the range of short: " + this.value);
        }
        return this.value.toShort();
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
    @Override
    public int intValue() {
        return this.value.toInt();
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
    @Override
    public int intValueExact() {
        if (!this.isIntValue()) {
            throw new ArithmeticException("Out of the range of int: " + this.value);
        }
        return this.value.toInt();
    }

    /**
     * Returns this JSON integral number as a Java primitive {@code long}.
     *
     * @return the {@code long} representation of this JSON integral number
     *
     * @since 0.10.42
     */
    @Override
    public long longValue() {
        return this.value.toLong();
    }

    /**
     * Returns this JSON integral number as a Java primitive {@code long}.
     *
     * @return the {@code long} representation of this JSON integral number
     *
     * @since 0.10.42
     */
    @Override
    public long longValueExact() {
        return this.value.toLong();
    }

    /**
     * Returns this JSON integral number as a {@link java.math.BigInteger}.
     *
     * @return the {@link java.math.BigInteger} representation of this JSON integral number
     *
     * @since 0.10.42
     */
    @Override
    public BigInteger bigIntegerValue() {
        return this.value.toBigInteger();
    }

    /**
     * Returns this JSON integral number as a {@link java.math.BigInteger}.
     *
     * @return the {@link java.math.BigInteger} representation of this JSON integral number
     *
     * @since 0.10.42
     */
    @Override
    public BigInteger bigIntegerValueExact() {
        return this.value.toBigInteger();
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
    @Override
    public float floatValue() {
        return this.value.toFloat();
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
    @Override
    public double doubleValue() {
        return this.value.toDouble();
    }

    /**
     * Returns this JSON integral number as a {@link java.math.BigDecimal}.
     *
     * @return the {@link java.math.BigDecimal} representation of this JSON integral number
     *
     * @since 0.10.42
     */
    @Override
    public BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf(this.value.toLong());
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
        return Long.toString(this.value.toLong());
    }

    /**
     * Returns the corresponding MessagePack's Integer value of this JSON integral number.
     *
     * @return the corresponding MessagePack's Integer value of this JSON integral number
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
        return this.value;
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
        return Long.toString(this.value.toLong());
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
        if (otherObject instanceof JsonLong) {
            final JsonLong other = (JsonLong) otherObject;
            return this.value.equals(other.value);
        }

        if (otherObject instanceof JsonDouble) {
            final JsonDouble other = (JsonDouble) otherObject;
            return other.isLongValue() && this.value.toLong() == other.longValue();
        }

        return false;
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
        return this.value.hashCode();
    }

    private final ImmutableLongValueImpl value;

    private final String literal;
}
