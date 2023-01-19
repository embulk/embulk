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

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

public class TestJsonObject {
    @Test
    public void testEmpty() {
        final JsonObject jsonObject = JsonObject.of();
        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonInteger());
        assertFalse(jsonObject.isJsonDecimal());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(0, jsonObject.size());
        assertEquals(0, jsonObject.entrySet().size());
        assertEquals(0, jsonObject.getKeyValueArray().length);
        assertFalse(jsonObject.entrySet().iterator().hasNext());
        assertThrows(NoSuchElementException.class, () -> jsonObject.entrySet().iterator().next());
        assertEquals("{}", jsonObject.toJson());
        assertEquals("{}", jsonObject.toString());
        assertEquals(JsonObject.of(), jsonObject);
    }

    @Test
    public void testOf() {
        final JsonObject jsonObject = JsonObject.of(
                JsonString.of("foo"), JsonInteger.of(456), JsonString.of("bar"), JsonInteger.of(456));
        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonInteger());
        assertFalse(jsonObject.isJsonDecimal());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(2, jsonObject.size());
        assertEquals(2, jsonObject.entrySet().size());
        assertEquals(4, jsonObject.getKeyValueArray().length);
        final Iterator<Map.Entry<String, JsonValue>> it = jsonObject.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonInteger.of(456)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonInteger.of(456)), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
        assertEquals("{\"foo\":456,\"bar\":456}", jsonObject.toJson());
        assertEquals("{\"foo\":456,\"bar\":456}", jsonObject.toString());
        assertEquals(JsonObject.of(
                         JsonString.of("foo"), JsonInteger.of(456),
                         JsonString.of("bar"), JsonInteger.of(456)),
                     jsonObject);
    }

    @Test
    public void testOfEntries() {
        final JsonObject jsonObject = JsonObject.ofEntries(
                new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonNull.of()),
                new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE)),
                new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonInteger.of(678)));
        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonInteger());
        assertFalse(jsonObject.isJsonDecimal());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(3, jsonObject.size());
        assertEquals(3, jsonObject.entrySet().size());
        assertEquals(6, jsonObject.getKeyValueArray().length);
        final Iterator<Map.Entry<String, JsonValue>> it = jsonObject.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonNull.of()), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonInteger.of(678)), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toJson());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toString());
        assertEquals(JsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonInteger.of(678)),
                     jsonObject);
    }

    @Test
    public void testSingleOfEntriesWithJsonStringKeys() {
        final JsonObject jsonObject = JsonObject.ofEntriesWithJsonStringKeys(
                new AbstractMap.SimpleEntry<JsonString, JsonValue>(JsonString.of("foo"), JsonNull.of()),
                new AbstractMap.SimpleEntry<JsonString, JsonValue>(JsonString.of("bar"), JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE)),
                new AbstractMap.SimpleEntry<JsonString, JsonValue>(JsonString.of("baz"), JsonInteger.of(678)));
        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonInteger());
        assertFalse(jsonObject.isJsonDecimal());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(3, jsonObject.size());
        assertEquals(3, jsonObject.entrySet().size());
        assertEquals(6, jsonObject.getKeyValueArray().length);
        final Iterator<Map.Entry<String, JsonValue>> it = jsonObject.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonNull.of()), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonInteger.of(678)), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toJson());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toString());
        assertEquals(JsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonInteger.of(678)),
                     jsonObject);
    }

    @Test
    public void testOfMap() {
        final LinkedHashMap<String, JsonValue> map = new LinkedHashMap<>();
        map.put("foo", JsonNull.of());
        map.put("bar", JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE));
        map.put("baz", JsonInteger.of(678));
        final JsonObject jsonObject = JsonObject.ofMap(map);
        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonInteger());
        assertFalse(jsonObject.isJsonDecimal());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(3, jsonObject.size());
        assertEquals(3, jsonObject.entrySet().size());
        assertEquals(6, jsonObject.getKeyValueArray().length);
        final Iterator<Map.Entry<String, JsonValue>> it = jsonObject.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonNull.of()), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonInteger.of(678)), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toJson());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toString());

        assertEquals(JsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonInteger.of(678)),
                     jsonObject);

        // Ordered differently.
        assertEquals(JsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("baz"), JsonInteger.of(678),
                             JsonString.of("bar"), JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE)),
                     jsonObject);

        // Different value.
        assertNotEquals(JsonObject.of(
                                JsonString.of("foo"), JsonNull.of(),
                                JsonString.of("baz"), JsonInteger.of(789),
                                JsonString.of("bar"), JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE)),
                        jsonObject);
    }

    @Test
    public void testOfMapWithJsonStringKeys() {
        final LinkedHashMap<JsonString, JsonValue> map = new LinkedHashMap<>();
        map.put(JsonString.of("foo"), JsonNull.of());
        map.put(JsonString.of("bar"), JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE));
        map.put(JsonString.of("baz"), JsonInteger.of(678));
        final JsonObject jsonObject = JsonObject.ofMapWithJsonStringKeys(map);
        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonInteger());
        assertFalse(jsonObject.isJsonDecimal());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(3, jsonObject.size());
        assertEquals(3, jsonObject.entrySet().size());
        assertEquals(6, jsonObject.getKeyValueArray().length);
        final Iterator<Map.Entry<String, JsonValue>> it = jsonObject.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonNull.of()), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonInteger.of(678)), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toJson());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toString());

        assertEquals(JsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonInteger.of(678)),
                     jsonObject);

        // Ordered differently.
        assertEquals(JsonObject.of(
                             JsonString.of("bar"), JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonInteger.of(678),
                             JsonString.of("foo"), JsonNull.of()),
                     jsonObject);

        // Different value.
        assertNotEquals(JsonObject.of(
                               JsonString.of("bar"), JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE),
                               JsonString.of("baz"), JsonInteger.of(234),
                               JsonString.of("foo"), JsonNull.of()),
                        jsonObject);
    }

    @Test
    public void testBuilder() {
        final LinkedHashMap<String, JsonValue> map1 = new LinkedHashMap<>();
        map1.put("hoge", JsonString.withLiteral("foo", "\"\\u0066oo\""));
        map1.put("fuga", JsonDecimal.of(123.4));
        final LinkedHashMap<JsonString, JsonValue> map2 = new LinkedHashMap<>();
        map2.put(JsonString.of("piyo"), JsonInteger.of(345));
        map2.put(JsonString.of("hogera"), JsonString.of("bar"));
        final JsonObject jsonObject = JsonObject.builder()
                .put("foo", JsonNull.of())
                .put(JsonString.of("bar"), JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE))
                .putEntry(new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonInteger.of(678)))
                .putEntryWithJsonStringKey(new AbstractMap.SimpleEntry<JsonString, JsonValue>(JsonString.of("qux"), JsonNull.of()))
                .putAll(map1)
                .putAllWithJsonStringKeys(map2)
                .build();

        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonInteger());
        assertFalse(jsonObject.isJsonDecimal());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonInteger());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDecimal());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(8, jsonObject.size());
        assertEquals(8, jsonObject.entrySet().size());
        assertEquals(16, jsonObject.getKeyValueArray().length);
        final Iterator<Map.Entry<String, JsonValue>> it = jsonObject.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonNull.of()), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonInteger.of(678)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("qux", JsonNull.of()), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("hoge", JsonString.of("foo")), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("fuga", JsonDecimal.of(123.4)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("piyo", JsonInteger.of(345)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("hogera", JsonString.of("bar")), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678,\"qux\":null,\"hoge\":\"\\u0066oo\",\"fuga\":123.4,\"piyo\":345,\"hogera\":\"bar\"}", jsonObject.toJson());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678,\"qux\":null,\"hoge\":\"foo\",\"fuga\":123.4,\"piyo\":345,\"hogera\":\"bar\"}", jsonObject.toString());
        assertEquals(JsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonInteger.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonInteger.of(678),
                             JsonString.of("qux"), JsonNull.of(),
                             JsonString.of("hoge"), JsonString.of("foo"),
                             JsonString.of("fuga"), JsonDecimal.of(123.4),
                             JsonString.of("piyo"), JsonInteger.of(345),
                             JsonString.of("hogera"), JsonString.of("bar")),
                     jsonObject);
    }

    @Test
    public void testInvalidOf() {
        final IllegalArgumentException ex1 = assertThrows(
                IllegalArgumentException.class, () -> JsonObject.of(JsonString.of("foo")));
        assertEquals("Even numbers of arguments must be specified to JsonObject#of(...).", ex1.getMessage());

        final IllegalArgumentException ex2 = assertThrows(
                IllegalArgumentException.class, () -> JsonObject.of(JsonString.of("foo"), JsonBoolean.TRUE, JsonBoolean.TRUE));
        assertEquals("Even numbers of arguments must be specified to JsonObject#of(...).", ex2.getMessage());

        final IllegalArgumentException ex3 = assertThrows(
                IllegalArgumentException.class, () -> JsonObject.of(JsonBoolean.TRUE, JsonBoolean.TRUE));
        assertEquals("JsonString must be specified as a key for JsonObject#of(...).", ex3.getMessage());
    }
}
