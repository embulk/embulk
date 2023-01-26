/*
 * Copyright 2023 The Embulk project
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;
import org.msgpack.value.ValueFactory;

public class TestJsonDouble {
    @Test
    public void testFinal() {
        // JsonDouble must be final.
        assertTrue(Modifier.isFinal(JsonDouble.class.getModifiers()));
    }

    @Test
    public void testNaN() {
        assertThrows(ArithmeticException.class, () -> JsonDouble.of(Double.NaN));
    }

    @Test
    public void testInfinity() {
        assertThrows(ArithmeticException.class, () -> JsonDouble.of(Double.NEGATIVE_INFINITY));
        assertThrows(ArithmeticException.class, () -> JsonDouble.of(Double.POSITIVE_INFINITY));
    }

    @Test
    public void testBasicDoubleValue() {
        final JsonDouble jsonDouble = JsonDouble.of(1234567890.123456);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertFalse(jsonDouble.isIntegral());
        assertFalse(jsonDouble.isByteValue());
        assertFalse(jsonDouble.isShortValue());
        assertFalse(jsonDouble.isIntValue());
        assertFalse(jsonDouble.isLongValue());
        assertEquals((byte) -46, jsonDouble.byteValue());  // Overflow & fractional
        assertThrows(ArithmeticException.class, () -> jsonDouble.byteValueExact());
        assertEquals((short) 722, jsonDouble.shortValue());  // Overflow & fractional
        assertThrows(ArithmeticException.class, () -> jsonDouble.shortValueExact());
        assertEquals((int) 1234567890, jsonDouble.intValue());  // Fractional
        assertThrows(ArithmeticException.class, () -> jsonDouble.intValueExact());
        assertEquals(1234567890L, jsonDouble.longValue());  // Fractional
        assertThrows(ArithmeticException.class, () -> jsonDouble.longValueExact());
        assertEquals(BigInteger.valueOf(1234567890L), jsonDouble.bigIntegerValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.bigIntegerValueExact());
        assertEquals((float) 1234567890.123456, jsonDouble.floatValue(), 0.001);
        assertEquals(1234567890.123456, jsonDouble.doubleValue(), 0.001);
        assertEquals(BigDecimal.valueOf(1234567890.123456), jsonDouble.bigDecimalValue());
        assertEquals("1.234567890123456E9", jsonDouble.toJson());
        assertEquals("1.234567890123456E9", jsonDouble.toString());
        assertEquals(JsonDouble.of(1234567890.123456), jsonDouble);

        assertEquals(ValueFactory.newFloat(1234567890.123456), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testZero() {
        final JsonDouble jsonDouble = JsonDouble.of(0.0);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertTrue(jsonDouble.isIntegral());
        assertTrue(jsonDouble.isByteValue());
        assertTrue(jsonDouble.isShortValue());
        assertTrue(jsonDouble.isIntValue());
        assertTrue(jsonDouble.isLongValue());
        assertEquals((byte) 0, jsonDouble.byteValue());
        assertEquals((byte) 0, jsonDouble.byteValueExact());
        assertEquals((short) 0, jsonDouble.shortValue());
        assertEquals((short) 0, jsonDouble.shortValueExact());
        assertEquals((int) 0, jsonDouble.intValue());
        assertEquals((int) 0, jsonDouble.intValueExact());
        assertEquals(0L, jsonDouble.longValue());
        assertEquals(0L, jsonDouble.longValueExact());
        assertEquals(BigInteger.ZERO, jsonDouble.bigIntegerValue());
        assertEquals(BigInteger.ZERO, jsonDouble.bigIntegerValueExact());
        assertEquals((float) 0.0, jsonDouble.floatValue());
        assertNotEquals((float) -0.0, jsonDouble.floatValue());
        assertEquals(0.0, jsonDouble.doubleValue());
        assertNotEquals(-0.0, jsonDouble.doubleValue());

        // BigDecimal#equals returns false if the scales of two BigDecimals are different.
        // https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html#equals-java.lang.Object-
        assertTrue(BigDecimal.ZERO.compareTo(jsonDouble.bigDecimalValue()) == 0);

        assertEquals("0.0", jsonDouble.toJson());
        assertEquals("0.0", jsonDouble.toString());
        assertEquals(JsonDouble.of(0.0), jsonDouble);

        assertEquals(ValueFactory.newFloat(0.0), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testNegativeZero() {
        final JsonDouble jsonDouble = JsonDouble.of(-0.0);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertTrue(jsonDouble.isIntegral());
        assertTrue(jsonDouble.isByteValue());
        assertTrue(jsonDouble.isShortValue());
        assertTrue(jsonDouble.isIntValue());
        assertTrue(jsonDouble.isLongValue());
        assertEquals((byte) 0, jsonDouble.byteValue());
        assertEquals((byte) 0, jsonDouble.byteValueExact());
        assertEquals((short) 0, jsonDouble.shortValue());
        assertEquals((short) 0, jsonDouble.shortValueExact());
        assertEquals((int) 0, jsonDouble.intValue());
        assertEquals((int) 0, jsonDouble.intValueExact());
        assertEquals(0L, jsonDouble.longValue());
        assertEquals(0L, jsonDouble.longValueExact());
        assertEquals(BigInteger.ZERO, jsonDouble.bigIntegerValue());
        assertEquals(BigInteger.ZERO, jsonDouble.bigIntegerValueExact());
        assertNotEquals((float) 0.0, jsonDouble.floatValue());
        assertEquals((float) -0.0, jsonDouble.floatValue());
        assertNotEquals(0.0, jsonDouble.doubleValue());
        assertEquals(-0.0, jsonDouble.doubleValue());

        // BigDecimal#equals returns false if the scales of two BigDecimals are different.
        // https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html#equals-java.lang.Object-
        assertTrue(BigDecimal.ZERO.compareTo(jsonDouble.bigDecimalValue()) == 0);

        assertEquals("-0.0", jsonDouble.toJson());
        assertEquals("-0.0", jsonDouble.toString());
        assertEquals(JsonDouble.of(0.0), jsonDouble);

        assertEquals(ValueFactory.newFloat(-0.0), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testOne() {
        final JsonDouble jsonDouble = JsonDouble.of(1.0);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertTrue(jsonDouble.isIntegral());
        assertTrue(jsonDouble.isByteValue());
        assertTrue(jsonDouble.isShortValue());
        assertTrue(jsonDouble.isIntValue());
        assertTrue(jsonDouble.isLongValue());
        assertEquals((byte) 1, jsonDouble.byteValue());
        assertEquals((byte) 1, jsonDouble.byteValueExact());
        assertEquals((short) 1, jsonDouble.shortValue());
        assertEquals((short) 1, jsonDouble.shortValueExact());
        assertEquals((int) 1, jsonDouble.intValue());
        assertEquals((int) 1, jsonDouble.intValueExact());
        assertEquals(1L, jsonDouble.longValue());
        assertEquals(1L, jsonDouble.longValueExact());
        assertEquals(BigInteger.ONE, jsonDouble.bigIntegerValue());
        assertEquals(BigInteger.ONE, jsonDouble.bigIntegerValueExact());
        assertEquals((float) 1.0, jsonDouble.floatValue());
        assertEquals(1.0, jsonDouble.doubleValue());

        // BigDecimal#equals returns false if the scales of two BigDecimals are different.
        // https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html#equals-java.lang.Object-
        assertTrue(BigDecimal.ONE.compareTo(jsonDouble.bigDecimalValue()) == 0);

        assertEquals("1.0", jsonDouble.toJson());
        assertEquals("1.0", jsonDouble.toString());
        assertEquals(JsonDouble.of(1.0), jsonDouble);

        assertEquals(ValueFactory.newFloat(1.0), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testNegativeOne() {
        final JsonDouble jsonDouble = JsonDouble.of(-1.0);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertTrue(jsonDouble.isIntegral());
        assertTrue(jsonDouble.isByteValue());
        assertTrue(jsonDouble.isShortValue());
        assertTrue(jsonDouble.isIntValue());
        assertTrue(jsonDouble.isLongValue());
        assertEquals((byte) -1, jsonDouble.byteValue());
        assertEquals((byte) -1, jsonDouble.byteValueExact());
        assertEquals((short) -1, jsonDouble.shortValue());
        assertEquals((short) -1, jsonDouble.shortValueExact());
        assertEquals((int) -1, jsonDouble.intValue());
        assertEquals((int) -1, jsonDouble.intValueExact());
        assertEquals(-1L, jsonDouble.longValue());
        assertEquals(-1L, jsonDouble.longValueExact());
        assertEquals(BigInteger.ONE.negate(), jsonDouble.bigIntegerValue());
        assertEquals(BigInteger.ONE.negate(), jsonDouble.bigIntegerValueExact());
        assertEquals((float) -1.0, jsonDouble.floatValue());
        assertEquals(-1.0, jsonDouble.doubleValue());

        // BigDecimal#equals returns false if the scales of two BigDecimals are different.
        // https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html#equals-java.lang.Object-
        assertTrue(BigDecimal.ONE.negate().compareTo(jsonDouble.bigDecimalValue()) == 0);

        assertEquals("-1.0", jsonDouble.toJson());
        assertEquals("-1.0", jsonDouble.toString());
        assertEquals(JsonDouble.of(-1.0), jsonDouble);

        assertEquals(ValueFactory.newFloat(-1.0), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testShortPi() {
        final JsonDouble jsonDouble = JsonDouble.of(3.1415);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertFalse(jsonDouble.isIntegral());
        assertFalse(jsonDouble.isByteValue());
        assertFalse(jsonDouble.isShortValue());
        assertFalse(jsonDouble.isIntValue());
        assertFalse(jsonDouble.isLongValue());
        assertEquals((byte) 3, jsonDouble.byteValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.byteValueExact());
        assertEquals((short) 3, jsonDouble.shortValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.shortValueExact());
        assertEquals((int) 3, jsonDouble.intValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.intValueExact());
        assertEquals(3L, jsonDouble.longValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.longValueExact());
        assertEquals(BigInteger.valueOf(3), jsonDouble.bigIntegerValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.bigIntegerValueExact());
        assertEquals((float) 3.1415, jsonDouble.floatValue(), 0.000001);
        assertEquals(3.1415, jsonDouble.doubleValue(), 0.000001);

        assertTrue(BigDecimal.valueOf(3.1415).setScale(5, RoundingMode.HALF_UP).compareTo(jsonDouble.bigDecimalValue()) == 0);

        assertEquals("3.1415", jsonDouble.toJson());
        assertEquals("3.1415", jsonDouble.toString());
        assertEquals(JsonDouble.of(3.1415), jsonDouble);

        assertEquals(ValueFactory.newFloat(3.1415), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testLongPi() {
        final JsonDouble jsonDouble = JsonDouble.of(Math.PI);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertFalse(jsonDouble.isIntegral());
        assertFalse(jsonDouble.isByteValue());
        assertFalse(jsonDouble.isShortValue());
        assertFalse(jsonDouble.isIntValue());
        assertFalse(jsonDouble.isLongValue());
        assertEquals((byte) 3, jsonDouble.byteValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.byteValueExact());
        assertEquals((short) 3, jsonDouble.shortValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.shortValueExact());
        assertEquals((int) 3, jsonDouble.intValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.intValueExact());
        assertEquals(3L, jsonDouble.longValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.longValueExact());
        assertEquals(BigInteger.valueOf(3), jsonDouble.bigIntegerValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.bigIntegerValueExact());
        assertEquals((float) Math.PI, jsonDouble.floatValue(), 0.0000001);
        assertEquals(Math.PI, jsonDouble.doubleValue(), 0.000001);

        assertTrue(BigDecimal.valueOf(Math.PI).setScale(12, RoundingMode.HALF_UP).compareTo(
                           jsonDouble.bigDecimalValue().setScale(12, RoundingMode.HALF_UP)) == 0);

        assertTrue(jsonDouble.toJson().startsWith("3.1415926535897"));
        assertTrue(jsonDouble.toString().startsWith("3.1415926535897"));
        assertEquals(JsonDouble.of(Math.PI), jsonDouble);

        assertEquals(ValueFactory.newFloat(Math.PI), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testBasicShortIntegralValue() {
        final JsonDouble jsonDouble = JsonDouble.of(19245.0);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertTrue(jsonDouble.isIntegral());
        assertFalse(jsonDouble.isByteValue());
        assertTrue(jsonDouble.isShortValue());
        assertTrue(jsonDouble.isIntValue());
        assertTrue(jsonDouble.isLongValue());
        assertEquals((byte) 45, jsonDouble.byteValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.byteValueExact());
        assertEquals((short) 19245, jsonDouble.shortValue());
        assertEquals((short) 19245, jsonDouble.shortValueExact());
        assertEquals((int) 19245, jsonDouble.intValue());
        assertEquals((int) 19245, jsonDouble.intValueExact());
        assertEquals(19245L, jsonDouble.longValue());
        assertEquals(19245L, jsonDouble.longValueExact());
        assertEquals(BigInteger.valueOf(19245), jsonDouble.bigIntegerValue());
        assertEquals(BigInteger.valueOf(19245), jsonDouble.bigIntegerValueExact());
        assertEquals((float) 19245.0, jsonDouble.floatValue(), 0.000001);
        assertEquals(19245.0, jsonDouble.doubleValue(), 0.000001);
        assertTrue(BigDecimal.valueOf(19245.0).setScale(5, RoundingMode.HALF_UP).compareTo(jsonDouble.bigDecimalValue()) == 0);

        assertEquals("19245.0", jsonDouble.toJson());
        assertEquals("19245.0", jsonDouble.toString());
        assertEquals(JsonDouble.of(19245.0), jsonDouble);

        assertEquals(ValueFactory.newFloat(19245.0), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testBasicShortDecimalValue() {
        final JsonDouble jsonDouble = JsonDouble.of(19245.12);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertFalse(jsonDouble.isIntegral());
        assertFalse(jsonDouble.isByteValue());
        assertFalse(jsonDouble.isShortValue());
        assertFalse(jsonDouble.isIntValue());
        assertFalse(jsonDouble.isLongValue());
        assertEquals((byte) 45, jsonDouble.byteValue());  // Overflow & fractional
        assertThrows(ArithmeticException.class, () -> jsonDouble.byteValueExact());
        assertEquals((short) 19245, jsonDouble.shortValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.shortValueExact());
        assertEquals((int) 19245, jsonDouble.intValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.intValueExact());
        assertEquals(19245L, jsonDouble.longValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.longValueExact());
        assertEquals(BigInteger.valueOf(19245), jsonDouble.bigIntegerValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.bigIntegerValueExact());
        assertEquals((float) 19245.12, jsonDouble.floatValue(), 0.000001);
        assertEquals(19245.12, jsonDouble.doubleValue(), 0.000001);
        assertTrue(BigDecimal.valueOf(19245.12).setScale(5, RoundingMode.HALF_UP).compareTo(jsonDouble.bigDecimalValue()) == 0);

        assertEquals("19245.12", jsonDouble.toJson());
        assertEquals("19245.12", jsonDouble.toString());
        assertEquals(JsonDouble.of(19245.12), jsonDouble);

        assertEquals(ValueFactory.newFloat(19245.12), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testBasicIntIntegralValue() {
        final JsonDouble jsonDouble = JsonDouble.of(9351902.0);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertTrue(jsonDouble.isIntegral());
        assertFalse(jsonDouble.isByteValue());
        assertFalse(jsonDouble.isShortValue());
        assertTrue(jsonDouble.isIntValue());
        assertTrue(jsonDouble.isLongValue());
        assertEquals((byte) -34, jsonDouble.byteValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.byteValueExact());
        assertEquals((short) -19746, jsonDouble.shortValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.shortValueExact());
        assertEquals((int) 9351902, jsonDouble.intValue());
        assertEquals((int) 9351902, jsonDouble.intValueExact());
        assertEquals(9351902L, jsonDouble.longValue());
        assertEquals(9351902L, jsonDouble.longValueExact());
        assertEquals(BigInteger.valueOf(9351902L), jsonDouble.bigIntegerValue());
        assertEquals(BigInteger.valueOf(9351902L), jsonDouble.bigIntegerValueExact());
        assertEquals((float) 9351902.0, jsonDouble.floatValue(), 0.000001);
        assertEquals(9351902.0, jsonDouble.doubleValue(), 0.000001);
        assertTrue(BigDecimal.valueOf(9351902.0).setScale(12, RoundingMode.HALF_UP).compareTo(jsonDouble.bigDecimalValue()) == 0);

        assertEquals("9351902.0", jsonDouble.toJson());
        assertEquals("9351902.0", jsonDouble.toString());
        assertEquals(JsonDouble.of(9351902.0), jsonDouble);

        assertEquals(ValueFactory.newFloat(9351902.0), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testBasicIntDecimalValue() {
        final JsonDouble jsonDouble = JsonDouble.of(9351902.523);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertFalse(jsonDouble.isIntegral());
        assertFalse(jsonDouble.isByteValue());
        assertFalse(jsonDouble.isShortValue());
        assertFalse(jsonDouble.isIntValue());
        assertFalse(jsonDouble.isLongValue());
        assertEquals((byte) -34, jsonDouble.byteValue());  // Overflow & fractional
        assertThrows(ArithmeticException.class, () -> jsonDouble.byteValueExact());
        assertEquals((short) -19746, jsonDouble.shortValue());  // Overflow & fractional
        assertThrows(ArithmeticException.class, () -> jsonDouble.shortValueExact());
        assertEquals((int) 9351902, jsonDouble.intValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.intValueExact());
        assertEquals(9351902L, jsonDouble.longValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.longValueExact());
        assertEquals(BigInteger.valueOf(9351902L), jsonDouble.bigIntegerValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.bigIntegerValueExact());
        assertEquals((float) 9351902.523, jsonDouble.floatValue(), 0.000001);
        assertEquals(9351902.523, jsonDouble.doubleValue(), 0.000001);
        assertTrue(BigDecimal.valueOf(9351902.523).setScale(12, RoundingMode.HALF_UP).compareTo(jsonDouble.bigDecimalValue()) == 0);

        assertEquals("9351902.523", jsonDouble.toJson());
        assertEquals("9351902.523", jsonDouble.toString());
        assertEquals(JsonDouble.of(9351902.523), jsonDouble);

        assertEquals(ValueFactory.newFloat(9351902.523), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testBasicLongIntegralValue() {
        final JsonDouble jsonDouble = JsonDouble.of(31234567890.0);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertTrue(jsonDouble.isIntegral());
        assertFalse(jsonDouble.isByteValue());
        assertFalse(jsonDouble.isShortValue());
        assertFalse(jsonDouble.isIntValue());
        assertTrue(jsonDouble.isLongValue());
        assertEquals((byte) -1, jsonDouble.byteValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.byteValueExact());
        assertEquals((short) -1, jsonDouble.shortValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.shortValueExact());
        assertEquals((int) 2147483647, jsonDouble.intValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.intValueExact());
        assertEquals(31234567890L, jsonDouble.longValue());
        assertEquals(31234567890L, jsonDouble.longValueExact());
        assertEquals(BigInteger.valueOf(31234567890L), jsonDouble.bigIntegerValue());
        assertEquals(BigInteger.valueOf(31234567890L), jsonDouble.bigIntegerValueExact());
        assertEquals((float) 31234567890.0, jsonDouble.floatValue(), 0.0001);
        assertEquals(31234567890.0, jsonDouble.doubleValue(), 0.0001);
        assertTrue(BigDecimal.valueOf(31234567890.0).setScale(12, RoundingMode.HALF_UP).compareTo(
                           jsonDouble.bigDecimalValue()) == 0);

        assertEquals("3.123456789E10", jsonDouble.toJson());
        assertEquals("3.123456789E10", jsonDouble.toString());
        assertEquals(JsonDouble.of(31234567890.0), jsonDouble);

        assertEquals(ValueFactory.newFloat(31234567890.0), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testBasicLongDecimalValue() {
        final JsonDouble jsonDouble = JsonDouble.of(31234567890.12);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertFalse(jsonDouble.isIntegral());
        assertFalse(jsonDouble.isByteValue());
        assertFalse(jsonDouble.isShortValue());
        assertFalse(jsonDouble.isIntValue());
        assertFalse(jsonDouble.isLongValue());
        assertEquals((byte) -1, jsonDouble.byteValue());  // Overflow & fractional
        assertThrows(ArithmeticException.class, () -> jsonDouble.byteValueExact());
        assertEquals((short) -1, jsonDouble.shortValue());  // Overflow & fractional
        assertThrows(ArithmeticException.class, () -> jsonDouble.shortValueExact());
        assertEquals((int) 2147483647, jsonDouble.intValue());  // Fractional
        assertThrows(ArithmeticException.class, () -> jsonDouble.intValueExact());
        assertEquals(31234567890L, jsonDouble.longValue());  // Fractional
        assertThrows(ArithmeticException.class, () -> jsonDouble.longValueExact());
        assertEquals(BigInteger.valueOf(31234567890L), jsonDouble.bigIntegerValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.bigIntegerValueExact());
        assertEquals((float) 31234567890.12, jsonDouble.floatValue(), 0.001);
        assertEquals(31234567890.12, jsonDouble.doubleValue(), 0.001);
        assertEquals(BigDecimal.valueOf(31234567890.12), jsonDouble.bigDecimalValue());
        assertEquals("3.123456789012E10", jsonDouble.toJson());
        assertEquals("3.123456789012E10", jsonDouble.toString());
        assertEquals(JsonDouble.of(31234567890.12), jsonDouble);

        assertEquals(ValueFactory.newFloat(31234567890.12), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testBasicOverLongIntegralValue() {
        final JsonDouble jsonDouble = JsonDouble.of(9_223_372_036_854_775_807_000.0);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertTrue(jsonDouble.isIntegral());
        assertFalse(jsonDouble.isByteValue());
        assertFalse(jsonDouble.isShortValue());
        assertFalse(jsonDouble.isIntValue());
        assertFalse(jsonDouble.isLongValue());
        assertEquals((byte) -1, jsonDouble.byteValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.byteValueExact());
        assertEquals((short) -1, jsonDouble.shortValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.shortValueExact());
        assertEquals((int) 2147483647, jsonDouble.intValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.intValueExact());
        assertEquals(9223372036854775807L, jsonDouble.longValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.longValueExact());
        assertEquals(new BigInteger("9223372036854776000000"), jsonDouble.bigIntegerValue());
        assertEquals(new BigInteger("9223372036854776000000"), jsonDouble.bigIntegerValueExact());
        assertEquals((float) 9.223372036854776E21, jsonDouble.floatValue(), 0.0001);
        assertEquals(9.223372036854776E21, jsonDouble.doubleValue(), 0.0001);
        assertTrue(BigDecimal.valueOf(9.223372036854776E21).setScale(12, RoundingMode.HALF_UP).compareTo(
                           jsonDouble.bigDecimalValue()) == 0);

        assertEquals("9.223372036854776E21", jsonDouble.toJson());
        assertEquals("9.223372036854776E21", jsonDouble.toString());
        assertEquals(JsonDouble.of(9.223372036854776E21), jsonDouble);

        assertEquals(ValueFactory.newFloat(9_223_372_036_854_775_807_000.0), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testBasicOverLongDecimalValue() {
        final JsonDouble jsonDouble = JsonDouble.of(9_223_372_036_854_775_807_000.12);  // .12 is dropped actually
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertTrue(jsonDouble.isIntegral());  // The fractional part is dropped due to precision, then it's considered integral.
        assertFalse(jsonDouble.isByteValue());
        assertFalse(jsonDouble.isShortValue());
        assertFalse(jsonDouble.isIntValue());
        assertFalse(jsonDouble.isLongValue());
        assertEquals((byte) -1, jsonDouble.byteValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.byteValueExact());
        assertEquals((short) -1, jsonDouble.shortValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.shortValueExact());
        assertEquals((int) 2147483647, jsonDouble.intValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.intValueExact());
        assertEquals(9223372036854775807L, jsonDouble.longValue());  // Overflow
        assertThrows(ArithmeticException.class, () -> jsonDouble.longValueExact());
        assertEquals(new BigInteger("9223372036854776000000"), jsonDouble.bigIntegerValue());
        assertEquals(new BigInteger("9223372036854776000000"), jsonDouble.bigIntegerValueExact());
        assertEquals((float) 9.223372036854776E21, jsonDouble.floatValue(), 0.0001);
        assertEquals(9.223372036854776E21, jsonDouble.doubleValue(), 0.0001);
        assertTrue(BigDecimal.valueOf(9.223372036854776E21).setScale(12, RoundingMode.HALF_UP).compareTo(
                           jsonDouble.bigDecimalValue()) == 0);

        assertEquals("9.223372036854776E21", jsonDouble.toJson());
        assertEquals("9.223372036854776E21", jsonDouble.toString());
        assertEquals(JsonDouble.of(9.223372036854776E21), jsonDouble);

        assertEquals(ValueFactory.newFloat(9_223_372_036_854_775_807_000.12), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testMinimunPositiveDecimal() {
        final JsonDouble jsonDouble = JsonDouble.of(Double.MIN_VALUE);
        assertEquals(JsonValue.EntityType.DOUBLE, jsonDouble.getEntityType());
        assertFalse(jsonDouble.isJsonNull());
        assertFalse(jsonDouble.isJsonBoolean());
        assertFalse(jsonDouble.isJsonLong());
        assertTrue(jsonDouble.isJsonDouble());
        assertFalse(jsonDouble.isJsonString());
        assertFalse(jsonDouble.isJsonArray());
        assertFalse(jsonDouble.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonLong());
        assertEquals(jsonDouble, jsonDouble.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonDouble.asJsonObject());
        assertEquals(8, jsonDouble.presumeReferenceSizeInBytes());
        assertFalse(jsonDouble.isIntegral());
        assertFalse(jsonDouble.isByteValue());
        assertFalse(jsonDouble.isShortValue());
        assertFalse(jsonDouble.isIntValue());
        assertFalse(jsonDouble.isLongValue());
        assertEquals((byte) 0, jsonDouble.byteValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.byteValueExact());
        assertEquals((short) 0, jsonDouble.shortValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.shortValueExact());
        assertEquals((int) 0, jsonDouble.intValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.intValueExact());
        assertEquals(0L, jsonDouble.longValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.longValueExact());
        assertEquals(BigInteger.valueOf(0), jsonDouble.bigIntegerValue());
        assertThrows(ArithmeticException.class, () -> jsonDouble.bigIntegerValueExact());
        assertEquals((float) 0.0, jsonDouble.floatValue());
        assertNotEquals(0.0, jsonDouble.doubleValue());
        assertEquals(0.0, jsonDouble.doubleValue(), 0.0000000000001);

        assertTrue(BigDecimal.ZERO.setScale(12, RoundingMode.HALF_UP).compareTo(
                           jsonDouble.bigDecimalValue().setScale(12, RoundingMode.HALF_UP)) == 0);

        assertEquals("4.9E-324", jsonDouble.toJson());
        assertEquals("4.9E-324", jsonDouble.toString());
        assertEquals(JsonDouble.of(Double.MIN_VALUE), jsonDouble);

        assertEquals(ValueFactory.newFloat(Double.MIN_VALUE), jsonDouble.toMsgpack());

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }

    @Test
    public void testFromMsgpack() {
        assertEquals(JsonDouble.of(0.0), JsonValue.fromMsgpack(ValueFactory.newFloat(0.0)));
        assertEquals(JsonDouble.of(-0.0), JsonValue.fromMsgpack(ValueFactory.newFloat(-0.0)));
        assertEquals(JsonDouble.of(12.41041), JsonValue.fromMsgpack(ValueFactory.newFloat(12.41041)));
        assertEquals(JsonDouble.of(Double.MIN_VALUE), JsonValue.fromMsgpack(ValueFactory.newFloat(Double.MIN_VALUE)));
    }
}
