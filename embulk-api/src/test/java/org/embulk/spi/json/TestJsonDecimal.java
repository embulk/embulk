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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

public class TestJsonDecimal {
    @Test
    public void testFinal() {
        // JsonDecimal must be final.
        assertTrue(Modifier.isFinal(JsonDecimal.class.getModifiers()));
    }

    @Test
    public void testBasicDoubleValue() {
        final JsonDecimal decimal = JsonDecimal.of(1234567890.123456);
        assertEquals(JsonValue.EntityType.DECIMAL, decimal.getEntityType());
        assertFalse(decimal.isJsonNull());
        assertFalse(decimal.isJsonBoolean());
        assertFalse(decimal.isJsonLong());
        assertTrue(decimal.isJsonDecimal());
        assertFalse(decimal.isJsonString());
        assertFalse(decimal.isJsonArray());
        assertFalse(decimal.isJsonObject());
        assertThrows(ClassCastException.class, () -> decimal.asJsonNull());
        assertThrows(ClassCastException.class, () -> decimal.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> decimal.asJsonLong());
        assertEquals(decimal, decimal.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> decimal.asJsonString());
        assertThrows(ClassCastException.class, () -> decimal.asJsonArray());
        assertThrows(ClassCastException.class, () -> decimal.asJsonObject());
        assertFalse(decimal.isIntegral());
        assertFalse(decimal.isByteValue());
        assertFalse(decimal.isShortValue());
        assertFalse(decimal.isIntValue());
        assertFalse(decimal.isLongValue());
        assertEquals((byte) -46, decimal.byteValue());  // Overflow & fractional
        assertThrows(ArithmeticException.class, () -> decimal.byteValueExact());
        assertEquals((short) 722, decimal.shortValue());  // Overflow & fractional
        assertThrows(ArithmeticException.class, () -> decimal.shortValueExact());
        assertEquals((int) 1234567890, decimal.intValue());  // Fractional
        assertThrows(ArithmeticException.class, () -> decimal.intValueExact());
        assertEquals(1234567890L, decimal.longValue());  // Fractional
        assertThrows(ArithmeticException.class, () -> decimal.longValueExact());
        assertEquals(BigInteger.valueOf(1234567890L), decimal.bigIntegerValue());
        assertThrows(ArithmeticException.class, () -> decimal.bigIntegerValueExact());
        assertEquals((float) 1234567890.123456, decimal.floatValue(), 0.001);
        assertEquals(1234567890.123456, decimal.doubleValue(), 0.001);
        assertEquals(BigDecimal.valueOf(1234567890.123456), decimal.bigDecimalValue());
        assertEquals("1.234567890123456E9", decimal.toJson());
        assertEquals("1.234567890123456E9", decimal.toString());
        assertEquals(JsonDecimal.of(1234567890.123456), decimal);

        // JsonDecimal#equals must normally reject a fake imitation of JsonDecimal.
        assertFalse(decimal.equals(FakeJsonDecimal.of(decimal.doubleValue())));
    }
}
