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
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.msgpack.value.ValueFactory;

public class TestJsonArray {
    @Test
    public void testFinal() {
        // JsonArray must be final.
        assertTrue(Modifier.isFinal(JsonArray.class.getModifiers()));
    }

    @Test
    public void testNull() {
        assertThrows(NullPointerException.class, () -> JsonArray.of((JsonValue[]) null));
        assertThrows(NullPointerException.class, () -> JsonArray.of(null, JsonString.of("foo")));
        assertThrows(NullPointerException.class, () -> JsonArray.ofList(null));

        final ArrayList<JsonValue> valuesNull = new ArrayList<>();
        valuesNull.add(JsonLong.of(987));
        valuesNull.add(null);
        assertThrows(NullPointerException.class, () -> JsonArray.ofList(valuesNull));
    }

    @Test
    public void testEmpty() {
        final JsonArray jsonArray = JsonArray.of();
        assertEquals(JsonValue.EntityType.ARRAY, jsonArray.getEntityType());
        assertFalse(jsonArray.isJsonNull());
        assertFalse(jsonArray.isJsonBoolean());
        assertFalse(jsonArray.isJsonLong());
        assertFalse(jsonArray.isJsonDouble());
        assertFalse(jsonArray.isJsonString());
        assertTrue(jsonArray.isJsonArray());
        assertFalse(jsonArray.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonString());
        assertEquals(jsonArray, jsonArray.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonObject());
        assertEquals(4, jsonArray.presumeReferenceSizeInBytes());
        assertEquals(0, jsonArray.size());
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(0));
        assertEquals("[]", jsonArray.toJson());
        assertEquals("[]", jsonArray.toString());
        assertEquals(JsonArray.of(), jsonArray);

        assertEquals(ValueFactory.emptyArray(), jsonArray.toMsgpack());

        // JsonArray#equals must normally reject a fake imitation of JsonArray.
        assertFalse(jsonArray.equals(FakeJsonArray.of()));
    }

    @Test
    public void testSingle() {
        final JsonArray jsonArray = JsonArray.of(JsonLong.of(987));
        assertEquals(JsonValue.EntityType.ARRAY, jsonArray.getEntityType());
        assertFalse(jsonArray.isJsonNull());
        assertFalse(jsonArray.isJsonBoolean());
        assertFalse(jsonArray.isJsonLong());
        assertFalse(jsonArray.isJsonDouble());
        assertFalse(jsonArray.isJsonString());
        assertTrue(jsonArray.isJsonArray());
        assertFalse(jsonArray.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonString());
        assertEquals(jsonArray, jsonArray.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonObject());
        assertEquals(12, jsonArray.presumeReferenceSizeInBytes());
        assertEquals(1, jsonArray.size());
        assertEquals(JsonLong.of(987), jsonArray.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(1));
        assertEquals("[987]", jsonArray.toJson());
        assertEquals("[987]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonLong.of(987)), jsonArray);

        assertEquals(ValueFactory.newArray(ValueFactory.newInteger(987)), jsonArray.toMsgpack());

        // JsonArray#equals must normally reject a fake imitation of JsonArray.
        assertFalse(jsonArray.equals(FakeJsonArray.of(JsonLong.of(987))));
    }

    @Test
    public void testMultiOfArray() {
        final JsonValue[] values = new JsonValue[3];
        values[0] = JsonLong.of(987);
        values[1] = JsonString.of("foo");
        values[2] = JsonBoolean.TRUE;
        final JsonArray jsonArray = JsonArray.of(values);
        assertEquals(JsonValue.EntityType.ARRAY, jsonArray.getEntityType());
        assertFalse(jsonArray.isJsonNull());
        assertFalse(jsonArray.isJsonBoolean());
        assertFalse(jsonArray.isJsonLong());
        assertFalse(jsonArray.isJsonDouble());
        assertFalse(jsonArray.isJsonString());
        assertTrue(jsonArray.isJsonArray());
        assertFalse(jsonArray.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonString());
        assertEquals(jsonArray, jsonArray.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonObject());
        assertEquals(23, jsonArray.presumeReferenceSizeInBytes());
        assertEquals(3, jsonArray.size());
        assertEquals(JsonLong.of(987), jsonArray.get(0));
        assertEquals(JsonString.of("foo"), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,\"foo\",true]", jsonArray.toJson());
        assertEquals("[987,\"foo\",true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonLong.of(987), JsonString.of("foo"), JsonBoolean.TRUE), jsonArray);

        // Tests that changing the original array does NOT affect the JsonArray instance.
        values[0] = JsonLong.of(1234);

        assertEquals(3, jsonArray.size());
        assertEquals(JsonLong.of(987), jsonArray.get(0));
        assertEquals(JsonString.of("foo"), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,\"foo\",true]", jsonArray.toJson());
        assertEquals("[987,\"foo\",true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonLong.of(987), JsonString.of("foo"), JsonBoolean.TRUE), jsonArray);

        assertEquals(
                ValueFactory.newArray(ValueFactory.newInteger(987), ValueFactory.newString("foo"), ValueFactory.newBoolean(true)),
                jsonArray.toMsgpack());

        // JsonArray#equals must normally reject a fake imitation of JsonArray.
        assertFalse(jsonArray.equals(FakeJsonArray.of(JsonLong.of(987), JsonString.of("foo"), JsonBoolean.TRUE)));
    }

    @Test
    public void testMultiOfList() {
        final ArrayList<JsonValue> values = new ArrayList<>();
        values.add(JsonLong.of(987));
        values.add(JsonString.of("foo"));
        values.add(JsonBoolean.TRUE);
        final JsonArray jsonArray = JsonArray.ofList(values);
        assertEquals(JsonValue.EntityType.ARRAY, jsonArray.getEntityType());
        assertFalse(jsonArray.isJsonNull());
        assertFalse(jsonArray.isJsonBoolean());
        assertFalse(jsonArray.isJsonLong());
        assertFalse(jsonArray.isJsonDouble());
        assertFalse(jsonArray.isJsonString());
        assertTrue(jsonArray.isJsonArray());
        assertFalse(jsonArray.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonString());
        assertEquals(jsonArray, jsonArray.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonObject());
        assertEquals(23, jsonArray.presumeReferenceSizeInBytes());
        assertEquals(3, jsonArray.size());
        assertEquals(JsonLong.of(987), jsonArray.get(0));
        assertEquals(JsonString.of("foo"), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,\"foo\",true]", jsonArray.toJson());
        assertEquals("[987,\"foo\",true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonLong.of(987), JsonString.of("foo"), JsonBoolean.TRUE), jsonArray);

        // Tests that changing the original array does NOT affect the JsonArray instance.
        values.add(JsonLong.of(1234));

        assertEquals(3, jsonArray.size());
        assertEquals(JsonLong.of(987), jsonArray.get(0));
        assertEquals(JsonString.of("foo"), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,\"foo\",true]", jsonArray.toJson());
        assertEquals("[987,\"foo\",true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonLong.of(987), JsonString.of("foo"), JsonBoolean.TRUE), jsonArray);

        assertEquals(
                ValueFactory.newArray(ValueFactory.newInteger(987), ValueFactory.newString("foo"), ValueFactory.newBoolean(true)),
                jsonArray.toMsgpack());

        // JsonArray#equals must normally reject a fake imitation of JsonArray.
        assertFalse(jsonArray.equals(FakeJsonArray.of(JsonLong.of(987), JsonString.of("foo"), JsonBoolean.TRUE)));
    }

    @Test
    public void testMultiUnsafe() {
        final JsonValue[] values = new JsonValue[3];
        values[0] = JsonLong.of(987);
        values[1] = JsonString.of("foo");
        values[2] = JsonBoolean.TRUE;
        final JsonArray jsonArray = JsonArray.ofUnsafe(values);
        assertEquals(JsonValue.EntityType.ARRAY, jsonArray.getEntityType());
        assertFalse(jsonArray.isJsonNull());
        assertFalse(jsonArray.isJsonBoolean());
        assertFalse(jsonArray.isJsonLong());
        assertFalse(jsonArray.isJsonDouble());
        assertFalse(jsonArray.isJsonString());
        assertTrue(jsonArray.isJsonArray());
        assertFalse(jsonArray.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonString());
        assertEquals(jsonArray, jsonArray.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonObject());
        assertEquals(23, jsonArray.presumeReferenceSizeInBytes());
        assertEquals(3, jsonArray.size());
        assertEquals(JsonLong.of(987), jsonArray.get(0));
        assertEquals(JsonString.of("foo"), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,\"foo\",true]", jsonArray.toJson());
        assertEquals("[987,\"foo\",true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonLong.of(987), JsonString.of("foo"), JsonBoolean.TRUE), jsonArray);

        // Tests that changing the original array DOES affect the JsonArray instance that is created by #ofUnsafe.
        values[0] = JsonLong.of(1234);

        assertEquals(3, jsonArray.size());
        assertEquals(JsonLong.of(1234), jsonArray.get(0));  // Updated
        assertEquals(JsonString.of("foo"), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[1234,\"foo\",true]", jsonArray.toJson());  // Updated
        assertEquals("[1234,\"foo\",true]", jsonArray.toString());  // Updated
        assertEquals(JsonArray.of(JsonLong.of(1234), JsonString.of("foo"), JsonBoolean.TRUE), jsonArray);  // Updated

        assertEquals(
                ValueFactory.newArray(ValueFactory.newInteger(1234), ValueFactory.newString("foo"), ValueFactory.newBoolean(true)),
                jsonArray.toMsgpack());

        // JsonArray#equals must normally reject a fake imitation of JsonArray.
        assertFalse(jsonArray.equals(FakeJsonArray.of(JsonLong.of(1234), JsonString.of("foo"), JsonBoolean.TRUE)));
    }

    @Test
    public void testNested() {
        final JsonValue[] values = new JsonValue[3];
        values[0] = JsonLong.of(987);
        values[1] = JsonArray.of(JsonString.of("foo"), JsonString.of("bar"), JsonString.of("baz"));
        values[2] = JsonBoolean.TRUE;
        final JsonArray jsonArray = JsonArray.of(values);
        assertEquals(JsonValue.EntityType.ARRAY, jsonArray.getEntityType());
        assertFalse(jsonArray.isJsonNull());
        assertFalse(jsonArray.isJsonBoolean());
        assertFalse(jsonArray.isJsonLong());
        assertFalse(jsonArray.isJsonDouble());
        assertFalse(jsonArray.isJsonString());
        assertTrue(jsonArray.isJsonArray());
        assertFalse(jsonArray.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonString());
        assertEquals(jsonArray, jsonArray.asJsonArray());
        assertThrows(ClassCastException.class, () -> jsonArray.asJsonObject());
        assertEquals(47, jsonArray.presumeReferenceSizeInBytes());
        assertEquals(3, jsonArray.size());
        assertEquals(JsonLong.of(987), jsonArray.get(0));
        assertEquals(JsonArray.of(JsonString.of("foo"), JsonString.of("bar"), JsonString.of("baz")), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,[\"foo\",\"bar\",\"baz\"],true]", jsonArray.toJson());
        assertEquals("[987,[\"foo\",\"bar\",\"baz\"],true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonLong.of(987), JsonArray.of(JsonString.of("foo"), JsonString.of("bar"), JsonString.of("baz")), JsonBoolean.TRUE), jsonArray);

        values[0] = JsonLong.of(1234);

        assertEquals(3, jsonArray.size());
        assertEquals(JsonLong.of(987), jsonArray.get(0));
        assertEquals(JsonArray.of(JsonString.of("foo"), JsonString.of("bar"), JsonString.of("baz")), jsonArray.get(1));
        assertEquals(JsonBoolean.TRUE, jsonArray.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> jsonArray.get(3));
        assertEquals("[987,[\"foo\",\"bar\",\"baz\"],true]", jsonArray.toJson());
        assertEquals("[987,[\"foo\",\"bar\",\"baz\"],true]", jsonArray.toString());
        assertEquals(JsonArray.of(JsonLong.of(987), JsonArray.of(JsonString.of("foo"), JsonString.of("bar"), JsonString.of("baz")), JsonBoolean.TRUE), jsonArray);

        assertEquals(
                ValueFactory.newArray(
                        ValueFactory.newInteger(987),
                        ValueFactory.newArray(
                                ValueFactory.newString("foo"), ValueFactory.newString("bar"), ValueFactory.newString("baz")),
                        ValueFactory.newBoolean(true)),
                jsonArray.toMsgpack());

        // JsonArray#equals must normally reject a fake imitation of JsonArray.
        assertFalse(jsonArray.equals(
                            FakeJsonArray.of(
                                    JsonLong.of(987),
                                    JsonArray.of(JsonString.of("foo"), JsonString.of("bar"), JsonString.of("baz")),
                                    JsonBoolean.TRUE)));
    }
}
