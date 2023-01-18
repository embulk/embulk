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

import org.junit.jupiter.api.Test;

public class TestJsonNull {
    @Test
    public void test() {
        final JsonNull jsonNull = JsonNull.of();
        assertEquals(JsonValue.EntityType.NULL, jsonNull.getEntityType());
        assertTrue(jsonNull.isJsonNull());
        assertFalse(jsonNull.isJsonBoolean());
        assertFalse(jsonNull.isJsonInteger());
        assertFalse(jsonNull.isJsonDecimal());
        assertFalse(jsonNull.isJsonString());
        assertFalse(jsonNull.isJsonArray());
        assertFalse(jsonNull.isJsonObject());
        assertEquals(jsonNull, jsonNull.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonNull.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonNull.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonNull.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonNull.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonNull.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonNull.asJsonObject());
        assertEquals("null", jsonNull.toJson());
        assertEquals("null", jsonNull.toString());
        assertEquals(JsonNull.of(), jsonNull);
    }
}
