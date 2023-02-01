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
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.msgpack.value.ValueFactory;

public class TestJsonObject {
    @Test
    public void testFinal() {
        // JsonObject must be final.
        assertTrue(Modifier.isFinal(JsonObject.class.getModifiers()));
    }

    @Test
    public void testEmpty() {
        final JsonObject jsonObject = JsonObject.of();
        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonLong());
        assertFalse(jsonObject.isJsonDouble());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(4, jsonObject.presumeReferenceSizeInBytes());
        assertEquals(0, jsonObject.size());
        assertEquals(0, jsonObject.entrySet().size());
        assertEquals(0, jsonObject.getKeyValueArray().length);
        assertFalse(jsonObject.entrySet().iterator().hasNext());
        assertThrows(NoSuchElementException.class, () -> jsonObject.entrySet().iterator().next());
        assertEquals("{}", jsonObject.toJson());
        assertEquals("{}", jsonObject.toString());
        assertEquals(JsonObject.of(), jsonObject);

        assertEquals(ValueFactory.emptyMap(), jsonObject.toMsgpack());

        // JsonObject#equals must normally reject a fake imitation of JsonObject.
        assertFalse(jsonObject.equals(FakeJsonObject.of()));
    }

    @Test
    public void testOf() {
        assertThrows(NullPointerException.class, () -> JsonObject.of(
                JsonString.of("foo"), null, JsonString.of("bar"), JsonLong.of(456)));
        assertThrows(NullPointerException.class, () -> JsonObject.of(
                JsonString.of("foo"), JsonLong.of(456), null, JsonLong.of(456)));

        final JsonObject jsonObject = JsonObject.of(
                JsonString.of("foo"), JsonLong.of(456), JsonString.of("bar"), JsonLong.of(456));
        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonLong());
        assertFalse(jsonObject.isJsonDouble());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(40, jsonObject.presumeReferenceSizeInBytes());
        assertEquals(2, jsonObject.size());
        assertEquals(2, jsonObject.entrySet().size());
        assertEquals(4, jsonObject.getKeyValueArray().length);
        final Iterator<Map.Entry<String, JsonValue>> it = jsonObject.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonLong.of(456)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonLong.of(456)), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
        assertEquals("{\"foo\":456,\"bar\":456}", jsonObject.toJson());
        assertEquals("{\"foo\":456,\"bar\":456}", jsonObject.toString());

        assertEquals(JsonObject.of(
                         JsonString.of("foo"), JsonLong.of(456),
                         JsonString.of("bar"), JsonLong.of(456)),
                     jsonObject);

        assertEquals(ValueFactory.newMap(
                         ValueFactory.newString("foo"), ValueFactory.newInteger(456),
                         ValueFactory.newString("bar"), ValueFactory.newInteger(456)),
                     jsonObject.toMsgpack());

        // JsonObject#equals must normally reject a fake imitation of JsonObject.
        assertFalse(jsonObject.equals(FakeJsonObject.of(
                         JsonString.of("foo"), JsonLong.of(456),
                         JsonString.of("bar"), JsonLong.of(456))));
    }

    @Test
    public void testOfEntries() {
        assertThrows(NullPointerException.class, () -> JsonObject.ofEntries((Map.Entry<String, JsonValue>[]) null));
        assertThrows(NullPointerException.class, () -> JsonObject.ofEntries(
                new AbstractMap.SimpleEntry<String, JsonValue>(null, JsonString.of("foo"))));
        assertThrows(NullPointerException.class, () -> JsonObject.ofEntries(
                new AbstractMap.SimpleEntry<String, JsonValue>("foo", null)));

        assertThrows(NullPointerException.class, () -> JsonObject.entry((String) null, JsonString.of("foo")));
        assertThrows(NullPointerException.class, () -> JsonObject.entry("foo", null));

        final JsonObject jsonObject = JsonObject.ofEntries(
                JsonObject.entry("foo", JsonNull.of()),
                JsonObject.entry("bar", JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE)),
                JsonObject.entry("baz", JsonLong.of(678)));
        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonLong());
        assertFalse(jsonObject.isJsonDouble());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(56, jsonObject.presumeReferenceSizeInBytes());
        assertEquals(3, jsonObject.size());
        assertEquals(3, jsonObject.entrySet().size());
        assertEquals(6, jsonObject.getKeyValueArray().length);
        final Iterator<Map.Entry<String, JsonValue>> it = jsonObject.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonNull.of()), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonLong.of(678)), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toJson());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toString());
        assertEquals(JsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonLong.of(678)),
                     jsonObject);

        assertEquals(ValueFactory.newMap(
                             ValueFactory.newString("foo"), ValueFactory.newNil(),
                             ValueFactory.newString("bar"), ValueFactory.newArray(ValueFactory.newInteger(123), ValueFactory.newBoolean(true)),
                             ValueFactory.newString("baz"), ValueFactory.newInteger(678)),
                     jsonObject.toMsgpack());

        // JsonObject#equals must normally reject a fake imitation of JsonObject.
        assertFalse(jsonObject.equals(FakeJsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonLong.of(678))));
    }

    @Test
    public void testSingleOfEntriesWithJsonStringKeys() {
        assertThrows(NullPointerException.class, () -> JsonObject.ofEntriesWithJsonStringKeys((Map.Entry<JsonString, JsonValue>[]) null));
        assertThrows(NullPointerException.class, () -> JsonObject.ofEntriesWithJsonStringKeys(
                new AbstractMap.SimpleEntry<JsonString, JsonValue>(null, JsonString.of("foo"))));
        assertThrows(NullPointerException.class, () -> JsonObject.ofEntriesWithJsonStringKeys(
                new AbstractMap.SimpleEntry<JsonString, JsonValue>(JsonString.of("foo"), null)));

        assertThrows(NullPointerException.class, () -> JsonObject.entry((JsonString) null, JsonString.of("foo")));
        assertThrows(NullPointerException.class, () -> JsonObject.entry(JsonString.of("foo"), null));

        final JsonObject jsonObject = JsonObject.ofEntriesWithJsonStringKeys(
                JsonObject.entry(JsonString.of("foo"), JsonNull.of()),
                JsonObject.entry(JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE)),
                JsonObject.entry(JsonString.of("baz"), JsonLong.of(678)));
        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonLong());
        assertFalse(jsonObject.isJsonDouble());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(56, jsonObject.presumeReferenceSizeInBytes());
        assertEquals(3, jsonObject.size());
        assertEquals(3, jsonObject.entrySet().size());
        assertEquals(6, jsonObject.getKeyValueArray().length);
        final Iterator<Map.Entry<String, JsonValue>> it = jsonObject.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonNull.of()), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonLong.of(678)), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toJson());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toString());

        assertEquals(JsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonLong.of(678)),
                     jsonObject);

        assertEquals(ValueFactory.newMap(
                             ValueFactory.newString("foo"), ValueFactory.newNil(),
                             ValueFactory.newString("bar"), ValueFactory.newArray(ValueFactory.newInteger(123), ValueFactory.newBoolean(true)),
                             ValueFactory.newString("baz"), ValueFactory.newInteger(678)),
                     jsonObject.toMsgpack());

        // JsonObject#equals must normally reject a fake imitation of JsonObject.
        assertFalse(jsonObject.equals(FakeJsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonLong.of(678))));
    }

    @Test
    public void testOfMap() {
        assertThrows(NullPointerException.class, () -> JsonObject.ofMap(null));

        final LinkedHashMap<String, JsonValue> mapNullKey = new LinkedHashMap<>();
        mapNullKey.put(null, JsonString.of("foo"));
        assertThrows(NullPointerException.class, () -> JsonObject.ofMap(mapNullKey));

        final LinkedHashMap<String, JsonValue> mapNullValue = new LinkedHashMap<>();
        mapNullValue.put("foo", null);
        assertThrows(NullPointerException.class, () -> JsonObject.ofMap(mapNullValue));

        final LinkedHashMap<String, JsonValue> map = new LinkedHashMap<>();
        map.put("foo", JsonNull.of());
        map.put("bar", JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE));
        map.put("baz", JsonLong.of(678));
        final JsonObject jsonObject = JsonObject.ofMap(map);
        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonLong());
        assertFalse(jsonObject.isJsonDouble());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(56, jsonObject.presumeReferenceSizeInBytes());
        assertEquals(3, jsonObject.size());
        assertEquals(3, jsonObject.entrySet().size());
        assertEquals(6, jsonObject.getKeyValueArray().length);
        final Iterator<Map.Entry<String, JsonValue>> it = jsonObject.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonNull.of()), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonLong.of(678)), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toJson());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toString());

        assertEquals(JsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonLong.of(678)),
                     jsonObject);

        assertEquals(ValueFactory.newMap(
                             ValueFactory.newString("foo"), ValueFactory.newNil(),
                             ValueFactory.newString("bar"), ValueFactory.newArray(ValueFactory.newInteger(123), ValueFactory.newBoolean(true)),
                             ValueFactory.newString("baz"), ValueFactory.newInteger(678)),
                     jsonObject.toMsgpack());

        // Ordered differently.
        assertEquals(JsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("baz"), JsonLong.of(678),
                             JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE)),
                     jsonObject);

        // Different value.
        assertNotEquals(JsonObject.of(
                                JsonString.of("foo"), JsonNull.of(),
                                JsonString.of("baz"), JsonLong.of(789),
                                JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE)),
                        jsonObject);

        // JsonObject#equals must normally reject a fake imitation of JsonObject.
        assertFalse(jsonObject.equals(FakeJsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("baz"), JsonLong.of(678),
                             JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE))));
    }

    @Test
    public void testOfMapWithJsonStringKeys() {
        assertThrows(NullPointerException.class, () -> JsonObject.ofMapWithJsonStringKeys(null));

        final LinkedHashMap<JsonString, JsonValue> mapNullKey = new LinkedHashMap<>();
        mapNullKey.put(null, JsonString.of("foo"));
        assertThrows(NullPointerException.class, () -> JsonObject.ofMapWithJsonStringKeys(mapNullKey));

        final LinkedHashMap<JsonString, JsonValue> mapNullValue = new LinkedHashMap<>();
        mapNullValue.put(JsonString.of("foo"), null);
        assertThrows(NullPointerException.class, () -> JsonObject.ofMapWithJsonStringKeys(mapNullValue));

        final LinkedHashMap<JsonString, JsonValue> map = new LinkedHashMap<>();
        map.put(JsonString.of("foo"), JsonNull.of());
        map.put(JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE));
        map.put(JsonString.of("baz"), JsonLong.of(678));
        final JsonObject jsonObject = JsonObject.ofMapWithJsonStringKeys(map);
        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonLong());
        assertFalse(jsonObject.isJsonDouble());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(56, jsonObject.presumeReferenceSizeInBytes());
        assertEquals(3, jsonObject.size());
        assertEquals(3, jsonObject.entrySet().size());
        assertEquals(6, jsonObject.getKeyValueArray().length);
        final Iterator<Map.Entry<String, JsonValue>> it = jsonObject.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonNull.of()), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonLong.of(678)), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toJson());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678}", jsonObject.toString());

        assertEquals(JsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonLong.of(678)),
                     jsonObject);

        assertEquals(ValueFactory.newMap(
                             ValueFactory.newString("foo"), ValueFactory.newNil(),
                             ValueFactory.newString("bar"), ValueFactory.newArray(ValueFactory.newInteger(123), ValueFactory.newBoolean(true)),
                             ValueFactory.newString("baz"), ValueFactory.newInteger(678)),
                     jsonObject.toMsgpack());

        // Ordered differently.
        assertEquals(JsonObject.of(
                             JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonLong.of(678),
                             JsonString.of("foo"), JsonNull.of()),
                     jsonObject);

        // Different value.
        assertNotEquals(JsonObject.of(
                               JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE),

                               JsonString.of("baz"), JsonLong.of(234),
                               JsonString.of("foo"), JsonNull.of()),
                        jsonObject);

        // JsonObject#equals must normally reject a fake imitation of JsonObject.
        assertFalse(jsonObject.equals(FakeJsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("baz"), JsonLong.of(678),
                             JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE))));
    }

    @Test
    public void testBuilderNull() {
        final JsonObject.Builder builder = JsonObject.builder();
        assertThrows(NullPointerException.class, () -> builder.put((String) null, JsonString.of("foo")));
        assertThrows(NullPointerException.class, () -> builder.put((JsonString) null, JsonString.of("foo")));
        assertThrows(NullPointerException.class, () -> builder.put("foo", null));
        assertThrows(NullPointerException.class, () -> builder.put(JsonString.of("foo"), null));
        assertThrows(NullPointerException.class, () -> builder.putEntry(null));
        assertThrows(NullPointerException.class, () -> builder.putEntry(
                         new AbstractMap.SimpleEntry<String, JsonValue>("foo", null)));
        assertThrows(NullPointerException.class, () -> builder.putEntry(
                         new AbstractMap.SimpleEntry<String, JsonValue>(null, JsonString.of("foo"))));
        assertThrows(NullPointerException.class, () -> builder.putEntryWithJsonStringKey(null));
        assertThrows(NullPointerException.class, () -> builder.putEntryWithJsonStringKey(
                         new AbstractMap.SimpleEntry<JsonString, JsonValue>(JsonString.of("foo"), null)));
        assertThrows(NullPointerException.class, () -> builder.putEntryWithJsonStringKey(
                         new AbstractMap.SimpleEntry<JsonString, JsonValue>(null, JsonString.of("foo"))));

        final LinkedHashMap<String, JsonValue> mapNullKey = new LinkedHashMap<>();
        mapNullKey.put(null, JsonString.of("foo"));
        assertThrows(NullPointerException.class, () -> builder.putAll(mapNullKey));

        final LinkedHashMap<String, JsonValue> mapNullValue = new LinkedHashMap<>();
        mapNullValue.put("foo", null);
        assertThrows(NullPointerException.class, () -> builder.putAll(mapNullValue));

        final LinkedHashMap<JsonString, JsonValue> mapJsonNullKey = new LinkedHashMap<>();
        mapJsonNullKey.put(null, JsonString.of("foo"));
        assertThrows(NullPointerException.class, () -> builder.putAllWithJsonStringKeys(mapJsonNullKey));

        final LinkedHashMap<JsonString, JsonValue> mapJsonNullValue = new LinkedHashMap<>();
        mapJsonNullValue.put(JsonString.of("foo"), null);
        assertThrows(NullPointerException.class, () -> builder.putAllWithJsonStringKeys(mapJsonNullValue));
    }

    @Test
    public void testBuilder() {
        final LinkedHashMap<String, JsonValue> map1 = new LinkedHashMap<>();
        map1.put("hoge", JsonString.withLiteral("foo", "\"\\u0066oo\""));
        map1.put("fuga", JsonDouble.of(123.4));
        final LinkedHashMap<JsonString, JsonValue> map2 = new LinkedHashMap<>();
        map2.put(JsonString.of("piyo"), JsonLong.of(345));
        map2.put(JsonString.of("hogera"), JsonString.of("bar"));
        final JsonObject jsonObject = JsonObject.builder()
                .put("foo", JsonNull.of())
                .put(JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE))
                .putEntry(new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonLong.of(678)))
                .putEntryWithJsonStringKey(new AbstractMap.SimpleEntry<JsonString, JsonValue>(JsonString.of("qux"), JsonNull.of()))
                .putAll(map1)
                .putAllWithJsonStringKeys(map2)
                .build();

        assertEquals(JsonValue.EntityType.OBJECT, jsonObject.getEntityType());
        assertFalse(jsonObject.isJsonNull());
        assertFalse(jsonObject.isJsonBoolean());
        assertFalse(jsonObject.isJsonLong());
        assertFalse(jsonObject.isJsonDouble());
        assertFalse(jsonObject.isJsonString());
        assertFalse(jsonObject.isJsonArray());
        assertTrue(jsonObject.isJsonObject());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonNull());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonBoolean());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonLong());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonDouble());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonString());
        assertThrows(ClassCastException.class, () -> jsonObject.asJsonArray());
        assertEquals(jsonObject, jsonObject.asJsonObject());
        assertEquals(175, jsonObject.presumeReferenceSizeInBytes());
        assertEquals(8, jsonObject.size());
        assertEquals(8, jsonObject.entrySet().size());
        assertEquals(16, jsonObject.getKeyValueArray().length);
        final Iterator<Map.Entry<String, JsonValue>> it = jsonObject.entrySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("foo", JsonNull.of()), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("bar", JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("baz", JsonLong.of(678)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("qux", JsonNull.of()), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("hoge", JsonString.of("foo")), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("fuga", JsonDouble.of(123.4)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("piyo", JsonLong.of(345)), it.next());
        assertTrue(it.hasNext());
        assertEquals(new AbstractMap.SimpleEntry<String, JsonValue>("hogera", JsonString.of("bar")), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, () -> it.next());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678,\"qux\":null,\"hoge\":\"\\u0066oo\",\"fuga\":123.4,\"piyo\":345,\"hogera\":\"bar\"}", jsonObject.toJson());
        assertEquals("{\"foo\":null,\"bar\":[123,true],\"baz\":678,\"qux\":null,\"hoge\":\"foo\",\"fuga\":123.4,\"piyo\":345,\"hogera\":\"bar\"}", jsonObject.toString());
        assertEquals(JsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonLong.of(678),
                             JsonString.of("qux"), JsonNull.of(),
                             JsonString.of("hoge"), JsonString.of("foo"),
                             JsonString.of("fuga"), JsonDouble.of(123.4),
                             JsonString.of("piyo"), JsonLong.of(345),
                             JsonString.of("hogera"), JsonString.of("bar")),
                     jsonObject);

        assertEquals(ValueFactory.newMap(
                             ValueFactory.newString("foo"), ValueFactory.newNil(),
                             ValueFactory.newString("bar"), ValueFactory.newArray(ValueFactory.newInteger(123), ValueFactory.newBoolean(true)),
                             ValueFactory.newString("baz"), ValueFactory.newInteger(678),
                             ValueFactory.newString("qux"), ValueFactory.newNil(),
                             ValueFactory.newString("hoge"), ValueFactory.newString("foo"),
                             ValueFactory.newString("fuga"), ValueFactory.newFloat(123.4),
                             ValueFactory.newString("piyo"), ValueFactory.newInteger(345),
                             ValueFactory.newString("hogera"), ValueFactory.newString("bar")),
                     jsonObject.toMsgpack());

        // JsonObject#equals must normally reject a fake imitation of JsonObject.
        assertFalse(jsonObject.equals(FakeJsonObject.of(
                             JsonString.of("foo"), JsonNull.of(),
                             JsonString.of("bar"), JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE),
                             JsonString.of("baz"), JsonLong.of(678),
                             JsonString.of("qux"), JsonNull.of(),
                             JsonString.of("hoge"), JsonString.of("foo"),
                             JsonString.of("fuga"), JsonDouble.of(123.4),
                             JsonString.of("piyo"), JsonLong.of(345),
                             JsonString.of("hogera"), JsonString.of("bar"))));
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

    @Test
    public void testOf1() {
        final JsonObject o1 = JsonObject.of("k1", JsonLong.of(1));
        assertEquals(1, o1.size());
        assertEquals(JsonLong.of(1), o1.get("k1"));
    }

    @Test
    public void testOf2() {
        final JsonObject o = JsonObject.of(
                "k1", JsonLong.of(1),
                "k2", JsonLong.of(2));
        assertEquals(2, o.size());
        assertEquals(JsonLong.of(1), o.get("k1"));
        assertEquals(JsonLong.of(2), o.get("k2"));
    }

    @Test
    public void testOf3() {
        final JsonObject o = JsonObject.of(
                "k1", JsonLong.of(1),
                "k2", JsonLong.of(2),
                "k3", JsonLong.of(3));
        assertEquals(3, o.size());
        assertEquals(JsonLong.of(1), o.get("k1"));
        assertEquals(JsonLong.of(2), o.get("k2"));
        assertEquals(JsonLong.of(3), o.get("k3"));
    }

    @Test
    public void testOf4() {
        final JsonObject o = JsonObject.of(
                "k1", JsonLong.of(1),
                "k2", JsonLong.of(2),
                "k3", JsonLong.of(3),
                "k4", JsonLong.of(4));
        assertEquals(4, o.size());
        assertEquals(JsonLong.of(1), o.get("k1"));
        assertEquals(JsonLong.of(2), o.get("k2"));
        assertEquals(JsonLong.of(3), o.get("k3"));
        assertEquals(JsonLong.of(4), o.get("k4"));
    }

    @Test
    public void testOf5() {
        final JsonObject o = JsonObject.of(
                "k1", JsonLong.of(1),
                "k2", JsonLong.of(2),
                "k3", JsonLong.of(3),
                "k4", JsonLong.of(4),
                "k5", JsonLong.of(5));
        assertEquals(5, o.size());
        assertEquals(JsonLong.of(1), o.get("k1"));
        assertEquals(JsonLong.of(2), o.get("k2"));
        assertEquals(JsonLong.of(3), o.get("k3"));
        assertEquals(JsonLong.of(4), o.get("k4"));
        assertEquals(JsonLong.of(5), o.get("k5"));
    }

    @Test
    public void testOf6() {
        final JsonObject o = JsonObject.of(
                "k1", JsonLong.of(1),
                "k2", JsonLong.of(2),
                "k3", JsonLong.of(3),
                "k4", JsonLong.of(4),
                "k5", JsonLong.of(5),
                "k6", JsonLong.of(6));
        assertEquals(6, o.size());
        assertEquals(JsonLong.of(1), o.get("k1"));
        assertEquals(JsonLong.of(2), o.get("k2"));
        assertEquals(JsonLong.of(3), o.get("k3"));
        assertEquals(JsonLong.of(4), o.get("k4"));
        assertEquals(JsonLong.of(5), o.get("k5"));
        assertEquals(JsonLong.of(6), o.get("k6"));
    }

    @Test
    public void testOf7() {
        final JsonObject o = JsonObject.of(
                "k1", JsonLong.of(1),
                "k2", JsonLong.of(2),
                "k3", JsonLong.of(3),
                "k4", JsonLong.of(4),
                "k5", JsonLong.of(5),
                "k6", JsonLong.of(6),
                "k7", JsonLong.of(7));
        assertEquals(7, o.size());
        assertEquals(JsonLong.of(1), o.get("k1"));
        assertEquals(JsonLong.of(2), o.get("k2"));
        assertEquals(JsonLong.of(3), o.get("k3"));
        assertEquals(JsonLong.of(4), o.get("k4"));
        assertEquals(JsonLong.of(5), o.get("k5"));
        assertEquals(JsonLong.of(6), o.get("k6"));
        assertEquals(JsonLong.of(7), o.get("k7"));
    }

    @Test
    public void testOf8() {
        final JsonObject o = JsonObject.of(
                "k1", JsonLong.of(1),
                "k2", JsonLong.of(2),
                "k3", JsonLong.of(3),
                "k4", JsonLong.of(4),
                "k5", JsonLong.of(5),
                "k6", JsonLong.of(6),
                "k7", JsonLong.of(7),
                "k8", JsonLong.of(8));
        assertEquals(8, o.size());
        assertEquals(JsonLong.of(1), o.get("k1"));
        assertEquals(JsonLong.of(2), o.get("k2"));
        assertEquals(JsonLong.of(3), o.get("k3"));
        assertEquals(JsonLong.of(4), o.get("k4"));
        assertEquals(JsonLong.of(5), o.get("k5"));
        assertEquals(JsonLong.of(6), o.get("k6"));
        assertEquals(JsonLong.of(7), o.get("k7"));
        assertEquals(JsonLong.of(8), o.get("k8"));
    }

    @Test
    public void testOf9() {
        final JsonObject o = JsonObject.of(
                "k1", JsonLong.of(1),
                "k2", JsonLong.of(2),
                "k3", JsonLong.of(3),
                "k4", JsonLong.of(4),
                "k5", JsonLong.of(5),
                "k6", JsonLong.of(6),
                "k7", JsonLong.of(7),
                "k8", JsonLong.of(8),
                "k9", JsonLong.of(9));
        assertEquals(9, o.size());
        assertEquals(JsonLong.of(1), o.get("k1"));
        assertEquals(JsonLong.of(2), o.get("k2"));
        assertEquals(JsonLong.of(3), o.get("k3"));
        assertEquals(JsonLong.of(4), o.get("k4"));
        assertEquals(JsonLong.of(5), o.get("k5"));
        assertEquals(JsonLong.of(6), o.get("k6"));
        assertEquals(JsonLong.of(7), o.get("k7"));
        assertEquals(JsonLong.of(8), o.get("k8"));
        assertEquals(JsonLong.of(9), o.get("k9"));
    }

    @Test
    public void testOf10() {
        final JsonObject o = JsonObject.of(
                "k1", JsonLong.of(1),
                "k2", JsonLong.of(2),
                "k3", JsonLong.of(3),
                "k4", JsonLong.of(4),
                "k5", JsonLong.of(5),
                "k6", JsonLong.of(6),
                "k7", JsonLong.of(7),
                "k8", JsonLong.of(8),
                "k9", JsonLong.of(9),
                "k10", JsonLong.of(10));
        assertEquals(10, o.size());
        assertEquals(JsonLong.of(1), o.get("k1"));
        assertEquals(JsonLong.of(2), o.get("k2"));
        assertEquals(JsonLong.of(3), o.get("k3"));
        assertEquals(JsonLong.of(4), o.get("k4"));
        assertEquals(JsonLong.of(5), o.get("k5"));
        assertEquals(JsonLong.of(6), o.get("k6"));
        assertEquals(JsonLong.of(7), o.get("k7"));
        assertEquals(JsonLong.of(8), o.get("k8"));
        assertEquals(JsonLong.of(9), o.get("k9"));
        assertEquals(JsonLong.of(10), o.get("k10"));
    }

    @Test
    public void testOfNulls() {
        assertThrows(NullPointerException.class, () -> JsonObject.of("k1", null));
        assertThrows(NullPointerException.class, () -> JsonObject.of(null, JsonNull.of()));
        assertThrows(NullPointerException.class, () -> JsonObject.of("k1", JsonString.of("f"), null, JsonNull.of()));
        assertThrows(NullPointerException.class, () -> JsonObject.of("k1", JsonLong.of(13423), "k2", null));
    }

    @Test
    public void testFromMsgpack() {
        assertEquals(
                JsonObject.of(
                             "foo", JsonNull.of(),
                             "bar", JsonArray.of(JsonLong.of(123), JsonBoolean.TRUE),
                             "baz", JsonObject.of(
                                     "sub1", JsonString.of("v1"),
                                     "sub2", JsonLong.of(42)),
                             "qux", JsonNull.of(),
                             "hoge", JsonString.of("foo"),
                             "fuga", JsonDouble.of(123.4),
                             "piyo", JsonLong.of(345),
                             "hogera", JsonString.of("bar")),
                JsonValue.fromMsgpack(ValueFactory.newMap(
                             ValueFactory.newString("foo"), ValueFactory.newNil(),
                             ValueFactory.newString("bar"), ValueFactory.newArray(ValueFactory.newInteger(123), ValueFactory.newBoolean(true)),
                             ValueFactory.newString("baz"), ValueFactory.newMap(
                                     ValueFactory.newString("sub1"), ValueFactory.newString("v1"),
                                     ValueFactory.newString("sub2"), ValueFactory.newInteger(42)),
                             ValueFactory.newString("qux"), ValueFactory.newNil(),
                             ValueFactory.newString("hoge"), ValueFactory.newString("foo"),
                             ValueFactory.newString("fuga"), ValueFactory.newFloat(123.4),
                             ValueFactory.newString("piyo"), ValueFactory.newInteger(345),
                             ValueFactory.newString("hogera"), ValueFactory.newString("bar"))));
    }
}
