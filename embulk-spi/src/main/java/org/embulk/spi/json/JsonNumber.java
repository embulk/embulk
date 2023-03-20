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
 * Represents a number in JSON.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format</a>
 *
 * @since 0.10.42
 */
public interface JsonNumber extends JsonValue {
    /**
     * Returns {@code true} if this JSON number is integral.
     *
     * @return {@code true} if this JSON number is integral
     *
     * @since 0.10.42
     */
    boolean isIntegral();

    /**
     * Returns {@code true} if the JSON number is integral in the range of {@code byte}, [-2<sup>7</sup> to 2<sup>7</sup>-1].
     *
     * @return {@code true} if the JSON number is integral in the range of {@code byte}
     *
     * @since 0.10.42
     */
    boolean isByteValue();

    /**
     * Returns {@code true} if the JSON number is integral in the range of {@code short}, [-2<sup>15</sup> to 2<sup>15</sup>-1].
     *
     * @return {@code true} if the JSON number is integral in the range of {@code short}
     *
     * @since 0.10.42
     */
    boolean isShortValue();

    /**
     * Returns {@code true} if the JSON number is integral in the range of {@code int}, [-2<sup>31</sup> to 2<sup>31</sup>-1].
     *
     * @return {@code true} if the JSON number is integral in the range of {@code int}
     *
     * @since 0.10.42
     */
    boolean isIntValue();

    /**
     * Returns {@code true} if the JSON number is integral in the range of {@code long}, [-2<sup>63</sup> to 2<sup>63</sup>-1].
     *
     * @return {@code true} if the JSON number is integral in the range of {@code long}
     *
     * @since 0.10.42
     */
    boolean isLongValue();

    /**
     * Returns this JSON number as a Java primitive {@code byte}.
     *
     * <p>It may narrow down the number to {@code byte} as a Java primitive. Note that this conversion can lose information
     * about the magnitude of this JSON number, precision, and range.
     *
     * @return the {@code byte} representation of this JSON number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Narrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    byte byteValue();

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
    byte byteValueExact();

    /**
     * Returns this JSON number as a Java primitive {@code short}.
     *
     * <p>It may narrow down the number to {@code short} as a Java primitive. Note that this conversion can lose information
     * about the magnitude of this JSON number, precision, and range.
     *
     * @return the {@code short} representation of this JSON number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Narrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    short shortValue();

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
    short shortValueExact();

    /**
     * Returns this JSON number as a Java primitive {@code int}.
     *
     * <p>It may narrow down the number to {@code int} as a Java primitive. Note that this conversion can lose information
     * about the magnitude of this JSON number, precision, and range.
     *
     * @return the {@code int} representation of this JSON number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Narrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    int intValue();

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
    int intValueExact();

    /**
     * Returns this JSON number as a Java primitive {@code long}.
     *
     * <p>It may narrow down the number to {@code long} as a Java primitive. Note that this conversion can lose information
     * about the magnitude of this JSON number, precision, and range.
     *
     * @return the {@code long} representation of this JSON number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Narrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    long longValue();

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
    long longValueExact();

    /**
     * Returns this JSON number as a {@link java.math.BigInteger}.
     *
     * <p>Note that this conversion loses the fractional part and the precision of the number.
     *
     * @return the {@link java.math.BigInteger} representation of this JSON number
     *
     * @since 0.10.42
     */
    BigInteger bigIntegerValue();

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
    BigInteger bigIntegerValueExact();

    /**
     * Returns this JSON number as a Java primitive {@code float}.
     *
     * <p>It may narrow down the number to {@code float} as a Java primitive. Note that this conversion can lose information
     * about the magnitude and the precision of the number.
     *
     * @return the {@code float} representation of this JSON number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Na
rrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    float floatValue();

    /**
     * Returns this JSON number as a Java primitive {@code double}.
     *
     * <p>It may narrow down the number to {@code double} as a Java primitive. Note that this conversion can lose information
     * about the magnitude and the precision of the number.
     *
     * @return the {@code double} representation of this JSON number
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.3">Java Language Specification - 5.1.3. Na
rrowing Primitive Conversion</a>
     *
     * @since 0.10.42
     */
    double doubleValue();

    /**
     * Returns this JSON number as a {@link java.math.BigDecimal}.
     *
     * @return the {@link java.math.BigDecimal} representation of this JSON number
     *
     * @since 0.10.42
     */
    BigDecimal bigDecimalValue();
}
