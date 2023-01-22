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

public class TestJsonDouble {
    @Test
    public void testFinal() {
        // JsonDouble must be final.
        assertTrue(Modifier.isFinal(JsonDouble.class.getModifiers()));
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

        // JsonDouble#equals must normally reject a fake imitation of JsonDouble.
        assertFalse(jsonDouble.equals(FakeJsonDouble.of(jsonDouble.doubleValue())));
    }
}
