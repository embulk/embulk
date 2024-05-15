package org.embulk.deps.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.spi.Exec;
import org.embulk.spi.time.TimestampParser;
import org.embulk.test.EmbulkTestRuntime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestConfigSource {
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private ConfigSource config;

    @Before
    public void setup() throws Exception {
        config = Exec.newConfigSource();
    }

    private static interface TypeFields extends Task {
        @Config("boolean")
        public boolean getBoolean();

        @Config("double")
        public double getDouble();

        @Config("int")
        public int getInt();

        @Config("long")
        public long getLong();

        @Config("string")
        public String getString();
    }

    private static interface OptionalFields extends Task {
        @Config("java_util_optional")
        @ConfigDefault("null")
        public java.util.Optional<String> getJavaUtilOptional();
    }

    private static interface DuplicationParent extends Task {
        @Config("duplicated_number")
        public int getInteger();
    }

    private static interface Duplicated extends DuplicationParent {
        @Config("duplicated_number")
        public String getString();

        @Config("duplicated_number")
        public double getDouble();
    }

    @SuppressWarnings("deprecation")
    private static interface DuplicatedDateTimeZone extends Task, TimestampParser.Task {
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        public String getDefaultTimeZoneId();

        @Config("dummy_value")
        public String getDummyValue();
    }

    @Test
    public void testSetGet() throws IOException {
        config.set("boolean", true);
        config.set("int", 3);
        config.set("double", 0.2);
        config.set("long", Long.MAX_VALUE);
        config.set("string", "sf");

        assertEquals(true, (boolean) config.get(boolean.class, "boolean"));
        assertEquals(3, (int) config.get(int.class, "int"));
        assertEquals(0.2, (double) config.get(double.class, "double"), 0.001);
        assertEquals(Long.MAX_VALUE, (long) config.get(long.class, "long"));
        assertEquals("sf", config.get(String.class, "string"));

        final JsonNode json = (new ObjectMapper()).readTree(config.toJson());
        assertTrue(json.isObject());
        final ArrayList<String> fieldNames = new ArrayList<>();
        json.fieldNames().forEachRemaining(fieldNames::add);
        Collections.sort(fieldNames);
        assertEquals(5, fieldNames.size());
        assertEquals("boolean", fieldNames.get(0));
        assertTrue(json.get("boolean").isBoolean());
        assertEquals(true, json.get("boolean").asBoolean());
        assertEquals("double", fieldNames.get(1));
        assertTrue(json.get("double").isDouble());
        assertEquals(0.2, json.get("double").asDouble(), 0.001);
        assertEquals("int", fieldNames.get(2));
        assertTrue(json.get("int").isInt());
        assertEquals(3, json.get("int").asInt());
        assertEquals("long", fieldNames.get(3));
        assertTrue(json.get("long").isLong());
        assertEquals(Long.MAX_VALUE, json.get("long").asLong());
        assertEquals("string", fieldNames.get(4));
        assertTrue(json.get("string").isTextual());
        assertEquals("sf", json.get("string").asText());
    }

    @Test
    public void testGetList() {
        setExample(this.config);

        assertFalse(this.config.hasList("does_not_exist"));
        assertFalse(this.config.hasList("boolean"));
        assertFalse(this.config.hasList("int"));
        assertFalse(this.config.hasList("string"));
        assertFalse(this.config.hasList("double"));
        assertTrue(this.config.hasList("variety_list"));
        assertTrue(this.config.hasList("string_list"));
        assertTrue(this.config.hasList("nested_list"));
        assertFalse(this.config.hasList("nested"));

        final List<?> varietyList = this.config.get(List.class, "variety_list");
        assertEquals(4, varietyList.size());
        assertTrue(varietyList.get(0) instanceof String);
        assertEquals("hoge", varietyList.get(0));
        assertTrue(varietyList.get(1) instanceof String);
        assertEquals("fuga", varietyList.get(1));
        assertTrue(varietyList.get(2) instanceof Map);
        assertTrue(varietyList.get(3) instanceof List);

        final List<String> stringList = this.config.getListOf(String.class, "string_list");
        assertEquals(2, stringList.size());
        assertEquals("hoge", stringList.get(0));
        assertEquals("fuga", stringList.get(1));

        final List<ConfigSource> nestedList = this.config.getListOf(ConfigSource.class, "nested_list");
        assertEquals(2, nestedList.size());
        final ConfigSource nested0 = nestedList.get(0);
        assertEquals(1, nested0.getAttributeNames().size());
        assertEquals("some", nested0.getAttributeNames().get(0));
        assertEquals("what", nested0.get(String.class, "some"));
        final ConfigSource nested1 = nestedList.get(1);
        assertEquals(1, nested1.getAttributeNames().size());
        assertEquals("else", nested1.getAttributeNames().get(0));
        assertEquals("where", nested1.get(String.class, "else"));
    }

    @Test
    public void testGetListOfWrongType() {
        setExample(this.config);

        try {
            this.config.getListOf(String.class, "variety_list");
        } catch (final RuntimeException ex) {
            assertTrue(ex.getMessage().startsWith(
                    "com.fasterxml.jackson.databind.exc.MismatchedInputException: "
                    + "Cannot deserialize value of type `java.lang.String` from Object value (token `JsonToken.START_OBJECT`)"));
        }
    }

    @Test
    public void testNested() {
        setExample(this.config);

        assertFalse(this.config.hasNested("does_not_exist"));
        assertFalse(this.config.hasNested("boolean"));
        assertFalse(this.config.hasNested("int"));
        assertFalse(this.config.hasNested("string"));
        assertFalse(this.config.hasNested("double"));
        assertFalse(this.config.hasNested("variety_list"));
        assertFalse(this.config.hasNested("string_list"));
        assertFalse(this.config.hasNested("nested_list"));
        assertTrue(this.config.hasNested("nested"));

        final ConfigSource nested = this.config.getNested("nested");
        assertEquals(2, nested.getAttributeNames().size());
        assertEquals("value1", nested.get(String.class, "key1"));
        assertEquals("value2", nested.get(String.class, "key2"));
    }

    @Test
    public void testToMap() {
        setExample(this.config);

        final Map<String, Object> map = this.config.toMap();
        assertEquals(8, map.size());
        assertTrue(map.get("boolean") instanceof Boolean);
        assertEquals(true, map.get("boolean"));
        assertTrue(map.get("int") instanceof Integer);
        assertEquals(12, map.get("int"));
        assertTrue(map.get("double") instanceof Double);
        assertEquals(42914.1420, map.get("double"));
        assertTrue(map.get("string") instanceof String);
        assertEquals("foo", map.get("string"));

        assertTrue(map.get("variety_list") instanceof List);
        final List<?> varietyList = (List<?>) map.get("variety_list");
        assertEquals(4, varietyList.size());
        assertEquals("hoge", varietyList.get(0));
        assertEquals("fuga", varietyList.get(1));
        assertTrue(varietyList.get(2) instanceof Map);
        final Map<String, Object> mapUnderVarietyList = (Map<String, Object>) varietyList.get(2);
        assertEquals(1, mapUnderVarietyList.size());
        assertEquals("something", mapUnderVarietyList.get("subkey1"));
        assertTrue(varietyList.get(3) instanceof List);
        final List<Object> listUnderVarietyList = (List<Object>) varietyList.get(3);
        assertEquals(1, listUnderVarietyList.size());
        assertEquals("somewhat", listUnderVarietyList.get(0));

        assertTrue(map.get("string_list") instanceof List);
        final List<?> stringList = (List<?>) map.get("string_list");
        assertEquals(2, stringList.size());
        assertEquals("hoge", stringList.get(0));
        assertEquals("fuga", stringList.get(1));

        assertTrue(map.get("nested_list") instanceof List);
        final List<?> nestedList = (List<?>) map.get("nested_list");
        assertEquals(2, nestedList.size());
        final Map<String, Object> nestedList0 = (Map<String, Object>) nestedList.get(0);
        assertEquals(1, nestedList0.size());
        assertEquals("what", nestedList0.get("some"));
        final Map<String, Object> nestedList1 = (Map<String, Object>) nestedList.get(1);
        assertEquals(1, nestedList1.size());
        assertEquals("where", nestedList1.get("else"));

        assertTrue(map.get("nested") instanceof Map);
        final Map<String, Object> nested = (Map<String, Object>) map.get("nested");
        assertEquals("value1", nested.get("key1"));
        assertEquals("value2", nested.get("key2"));
    }

    @Test
    public void testOptionalPresent() {
        config.set("java_util_optional", "JavaUtil");

        final OptionalFields loaded = config.loadConfig(OptionalFields.class);
        assertTrue(loaded.getJavaUtilOptional().isPresent());
        assertEquals("JavaUtil", loaded.getJavaUtilOptional().get());
    }

    @Test
    public void testOptionalAbsent() {
        final OptionalFields loaded = config.loadConfig(OptionalFields.class);
        assertFalse(loaded.getJavaUtilOptional().isPresent());
    }

    @Test
    public void testLoadConfig() {
        config.set("boolean", true);
        config.set("int", 3);
        config.set("double", 0.2);
        config.set("long", Long.MAX_VALUE);
        config.set("string", "sf");

        TypeFields task = config.loadConfig(TypeFields.class);
        assertEquals(true, task.getBoolean());
        assertEquals(3, task.getInt());
        assertEquals(0.2, task.getDouble(), 0.001);
        assertEquals(Long.MAX_VALUE, task.getLong());
        assertEquals("sf", task.getString());
    }

    @Test
    public void testDuplicatedConfigName() {
        config.set("duplicated_number", "1034");

        Duplicated task = config.loadConfig(Duplicated.class);
        assertEquals(1034, task.getInteger());
        assertEquals("1034", task.getString());
        assertEquals(1034.0, task.getDouble(), 0.000001);
    }

    @Test
    public void testDuplicatedDateTimeZone() {
        config.set("default_timezone", "Asia/Tokyo");
        config.set("default_timestamp_format", "%Y");
        config.set("dummy_value", "foobar");

        DuplicatedDateTimeZone task = config.loadConfig(DuplicatedDateTimeZone.class);
        assertEquals("Asia/Tokyo", task.getDefaultTimeZoneId());
        assertEquals("%Y", task.getDefaultTimestampFormat());
        assertEquals("1970-01-01", task.getDefaultDate());
        assertEquals("foobar", task.getDummyValue());
    }

    @Test
    public void testDuplicatedDateTimeZoneWithDefault() {
        config.set("default_timestamp_format", "%Y");
        config.set("dummy_value", "foobar");

        DuplicatedDateTimeZone task = config.loadConfig(DuplicatedDateTimeZone.class);
        assertEquals("UTC", task.getDefaultTimeZoneId());
        assertEquals("%Y", task.getDefaultTimestampFormat());
        assertEquals("1970-01-01", task.getDefaultDate());
        assertEquals("foobar", task.getDummyValue());
    }

    private static interface ValidateFields extends Task {
        @Config("valid")
        public String getValid();
    }

    @Test
    public void testValidatePasses() {
        config.set("valid", "data");
        ValidateFields task = config.loadConfig(ValidateFields.class);
        task.validate();
        assertEquals("data", task.getValid());
    }

    @Test(expected = ConfigException.class)
    public void testDefaultValueValidateFails() {
        ValidateFields task = config.loadConfig(ValidateFields.class);
        task.validate();
    }

    // TODO test Min, Max, and other validations

    private static interface SimpleFields extends Task {
        @Config("type")
        public String getType();
    }

    @Test
    public void testFromJson() {
        String json = "{\"type\":\"test\"}";
        // TODO
    }

    private static void setExample(final ConfigSource config) {
        config.set("boolean", true);
        config.set("int", 12);
        config.set("double", 42914.1420);
        config.set("string", "foo");

        final ArrayList<Object> varietyList = new ArrayList<>();
        varietyList.add("hoge");
        varietyList.add("fuga");
        final ConfigSource nestedUnderVarietyList = Exec.newConfigSource();
        nestedUnderVarietyList.set("subkey1", "something");
        varietyList.add(nestedUnderVarietyList);
        final ArrayList<String> listUnderVarietyList = new ArrayList<>();
        listUnderVarietyList.add("somewhat");
        varietyList.add(listUnderVarietyList);
        config.set("variety_list", varietyList);

        final ArrayList<String> stringList = new ArrayList<>();
        stringList.add("hoge");
        stringList.add("fuga");
        config.set("string_list", stringList);

        final ArrayList<ConfigSource> nestedList = new ArrayList<>();
        final ConfigSource nested0UnderNestedList = Exec.newConfigSource();
        nested0UnderNestedList.set("some", "what");
        nestedList.add(nested0UnderNestedList);
        final ConfigSource nested1UnderNestedList = Exec.newConfigSource();
        nested1UnderNestedList.set("else", "where");
        nestedList.add(nested1UnderNestedList);
        config.set("nested_list", nestedList);

        final ConfigSource nested = Exec.newConfigSource();
        nested.set("key1", "value1");
        nested.set("key2", "value2");
        config.set("nested", nested);
    }
}
