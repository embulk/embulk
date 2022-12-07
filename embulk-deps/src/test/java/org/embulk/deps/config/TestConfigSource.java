package org.embulk.deps.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.spi.Exec;
import org.embulk.spi.time.TimestampParser;
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
}
