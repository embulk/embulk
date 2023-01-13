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

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class TestJsonArray {
    @Test
    public void testEmpty() {
        final JsonArray jsonArray = JsonArray.of();
        assertEquals(JsonValue.Type.ARRAY, jsonArray.getType());
        assertFalse(jsonArray.isNull());
        assertFalse(jsonArray.isBoolean());
        assertFalse(jsonArray.isInteger());
        assertFalse(jsonArray.isDecimal());
        assertFalse(jsonArray.isString());
        assertTrue(jsonArray.isArray());
        assertFalse(jsonArray.isObject());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonString());
        assertEquals(jsonArray, jsonArray.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonObject());
        assertEquals(0, jsonArray.size());
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(0));
        assertEquals("[]", jsonArray.toJson());
        assertEquals("[]", jsonArray.toString());
        assertEquals(JsonArray.of(), jsonArray);
    }

    @Test
    public void testSingle() {
        final JsonArray jsonArray = JsonArray.of(JsonInteger.of(987));
        assertEquals(JsonValue.Type.ARRAY, jsonArray.getType());
        assertFalse(jsonArray.isNull());
        assertFalse(jsonArray.isBoolean());
        assertFalse(jsonArray.isInteger());
        assertFalse(jsonArray.isDecimal());
        assertFalse(jsonArray.isString());
        assertTrue(jsonArray.isArray());
        assertFalse(jsonArray.isObject());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonString());
        assertEquals(jsonArray, jsonArray.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonObject());
        assertEquals(1, jsonArray.size());
        assertEquals(JsonInteger.of(987), jsonArray.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(1));
        assertEquals("[987]", jsonArray.toJson());
        assertEquals("[987]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonInteger.of(987)), jsonArray);
    }

    @Test
    public void testMultiOfArray() {
        final JsonValue[] values = new JsonValue[3];
        values[0] = JsonInteger.of(987);
        values[1] = JsonString.of("foo");
        values[2] = JsonBoolean.TRUE;
        final JsonArray jsonArray = JsonArray.of(values);
        assertEquals(JsonValue.Type.ARRAY, jsonArray.getType());
        assertFalse(jsonArray.isNull());
        assertFalse(jsonArray.isBoolean());
        assertFalse(jsonArray.isInteger());
        assertFalse(jsonArray.isDecimal());
        assertFalse(jsonArray.isString());
        assertTrue(jsonArray.isArray());
        assertFalse(jsonArray.isObject());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonString());
        assertEquals(jsonArray, jsonArray.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonObject());
        assertEquals(3, jsonArray.size());
        assertEquals(JsonInteger.of(987), jsonArray.get(0));
        assertEquals(JsonString.of("foo"), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,\"foo\",true]", jsonArray.toJson());
        assertEquals("[987,\"foo\",true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonInteger.of(987), JsonString.of("foo"), JsonBoolean.TRUE), jsonArray);

        values[0] = JsonInteger.of(1234);

        assertEquals(3, jsonArray.size());
        assertEquals(JsonInteger.of(987), jsonArray.get(0));
        assertEquals(JsonString.of("foo"), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,\"foo\",true]", jsonArray.toJson());
        assertEquals("[987,\"foo\",true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonInteger.of(987), JsonString.of("foo"), JsonBoolean.TRUE), jsonArray);
    }

    @Test
    public void testMultiOfList() {
        final ArrayList<JsonValue> values = new ArrayList<>();
        values.add(JsonInteger.of(987));
        values.add(JsonString.of("foo"));
        values.add(JsonBoolean.TRUE);
        final JsonArray jsonArray = JsonArray.ofList(values);
        assertEquals(JsonValue.Type.ARRAY, jsonArray.getType());
        assertFalse(jsonArray.isNull());
        assertFalse(jsonArray.isBoolean());
        assertFalse(jsonArray.isInteger());
        assertFalse(jsonArray.isDecimal());
        assertFalse(jsonArray.isString());
        assertTrue(jsonArray.isArray());
        assertFalse(jsonArray.isObject());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonString());
        assertEquals(jsonArray, jsonArray.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonObject());
        assertEquals(3, jsonArray.size());
        assertEquals(JsonInteger.of(987), jsonArray.get(0));
        assertEquals(JsonString.of("foo"), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,\"foo\",true]", jsonArray.toJson());
        assertEquals("[987,\"foo\",true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonInteger.of(987), JsonString.of("foo"), JsonBoolean.TRUE), jsonArray);

        values.add(JsonInteger.of(1234));

        assertEquals(3, jsonArray.size());
        assertEquals(JsonInteger.of(987), jsonArray.get(0));
        assertEquals(JsonString.of("foo"), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,\"foo\",true]", jsonArray.toJson());
        assertEquals("[987,\"foo\",true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonInteger.of(987), JsonString.of("foo"), JsonBoolean.TRUE), jsonArray);
    }


    @Test
    public void testMultiUnsafe() {
        final JsonValue[] values = new JsonValue[3];
        values[0] = JsonInteger.of(987);
        values[1] = JsonString.of("foo");
        values[2] = JsonBoolean.TRUE;
        final JsonArray jsonArray = JsonArray.ofUnsafe(values);
        assertEquals(JsonValue.Type.ARRAY, jsonArray.getType());
        assertFalse(jsonArray.isNull());
        assertFalse(jsonArray.isBoolean());
        assertFalse(jsonArray.isInteger());
        assertFalse(jsonArray.isDecimal());
        assertFalse(jsonArray.isString());
        assertTrue(jsonArray.isArray());
        assertFalse(jsonArray.isObject());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonString());
        assertEquals(jsonArray, jsonArray.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonObject());
        assertEquals(3, jsonArray.size());
        assertEquals(JsonInteger.of(987), jsonArray.get(0));
        assertEquals(JsonString.of("foo"), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,\"foo\",true]", jsonArray.toJson());
        assertEquals("[987,\"foo\",true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonInteger.of(987), JsonString.of("foo"), JsonBoolean.TRUE), jsonArray);

        values[0] = JsonInteger.of(1234);

        assertEquals(3, jsonArray.size());
        assertEquals(JsonInteger.of(1234), jsonArray.get(0));  // Updated
        assertEquals(JsonString.of("foo"), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[1234,\"foo\",true]", jsonArray.toJson());  // Updated
        assertEquals("[1234,\"foo\",true]", jsonArray.toString());  // Updated
        assertEquals(JsonArray.of(JsonInteger.of(1234), JsonString.of("foo"), JsonBoolean.TRUE), jsonArray);  // Updated
    }

    @Test
    public void testNested() {
        final JsonValue[] values = new JsonValue[3];
        values[0] = JsonInteger.of(987);
        values[1] = JsonArray.of(JsonString.of("foo"), JsonString.of("bar"), JsonString.of("baz"));
        values[2] = JsonBoolean.TRUE;
        final JsonArray jsonArray = JsonArray.of(values);
        assertEquals(JsonValue.Type.ARRAY, jsonArray.getType());
        assertFalse(jsonArray.isNull());
        assertFalse(jsonArray.isBoolean());
        assertFalse(jsonArray.isInteger());
        assertFalse(jsonArray.isDecimal());
        assertFalse(jsonArray.isString());
        assertTrue(jsonArray.isArray());
        assertFalse(jsonArray.isObject());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonString());
        assertEquals(jsonArray, jsonArray.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonObject());
        assertEquals(3, jsonArray.size());
        assertEquals(JsonInteger.of(987), jsonArray.get(0));
        assertEquals(JsonArray.of(JsonString.of("foo"), JsonString.of("bar"), JsonString.of("baz")), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,[\"foo\",\"bar\",\"baz\"],true]", jsonArray.toJson());
        assertEquals("[987,[\"foo\",\"bar\",\"baz\"],true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonInteger.of(987), JsonArray.of(JsonString.of("foo"), JsonString.of("bar"), JsonString.of("baz")), JsonBoolean.TRUE), jsonArray);

        values[0] = JsonInteger.of(1234);

        assertEquals(3, jsonArray.size());
        assertEquals(JsonInteger.of(987), jsonArray.get(0));
        assertEquals(JsonArray.of(JsonString.of("foo"), JsonString.of("bar"), JsonString.of("baz")), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,[\"foo\",\"bar\",\"baz\"],true]", jsonArray.toJson());
        assertEquals("[987,[\"foo\",\"bar\",\"baz\"],true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonInteger.of(987), JsonArray.of(JsonString.of("foo"), JsonString.of("bar"), JsonString.of("baz")), JsonBoolean.TRUE), jsonArray);
    }
}
