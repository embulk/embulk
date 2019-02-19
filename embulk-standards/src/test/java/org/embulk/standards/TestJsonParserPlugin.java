package org.embulk.standards;

import static org.embulk.standards.JsonParserPlugin.InvalidEscapeStringPolicy.PASSTHROUGH;
import static org.embulk.standards.JsonParserPlugin.InvalidEscapeStringPolicy.SKIP;
import static org.embulk.standards.JsonParserPlugin.InvalidEscapeStringPolicy.UNESCAPE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.msgpack.value.ValueFactory.newArray;
import static org.msgpack.value.ValueFactory.newBoolean;
import static org.msgpack.value.ValueFactory.newInteger;
import static org.msgpack.value.ValueFactory.newMap;
import static org.msgpack.value.ValueFactory.newString;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.spi.DataException;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInput;
import org.embulk.spi.Schema;
import org.embulk.spi.TestPageBuilderReader.MockPageOutput;
import org.embulk.spi.json.JsonParser;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.util.InputStreamFileInput;
import org.embulk.spi.util.Pages;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.msgpack.value.Value;

public class TestJsonParserPlugin {
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private ConfigSource config;
    private JsonParserPlugin plugin;
    private MockPageOutput output;

    @Before
    public void createResource() {
        config = config();
        plugin = new JsonParserPlugin();
        output = new MockPageOutput();
    }

    @Test
    public void checkDefaultValues() {
        ConfigSource config = Exec.newConfigSource();

        JsonParserPlugin.PluginTask task = config.loadConfig(JsonParserPlugin.PluginTask.class);
        assertEquals(false, task.getStopOnInvalidRecord());
        assertEquals(JsonParserPlugin.InvalidEscapeStringPolicy.PASSTHROUGH, task.getInvalidEscapeStringPolicy());
    }

    @Test
    public void readNormalJson() throws Exception {
        transaction(config, fileInput(
                "{\"_c0\":true,\"_c1\":10,\"_c2\":\"embulk\",\"_c3\":{\"k\":\"v\"}}",
                "{}",
                "{\n"
                        + "\"_c0\":false,\n"
                        + "\"_c1\":-10,\n"
                        + "\"_c2\":\"エンバルク\",\n"
                        + "\"_c3\":[\"e0\",\"e1\"]\n"
                        + "}",
                "[1, 2, 3]", // this line should be skipped.
                "\"embulk\"", // this line should be skipped.
                "10", // this line should be skipped.
                "true", // this line should be skipped.
                "false", // this line should be skipped.
                "null" // this line should be skipped.
        ));

        List<Object[]> records = Pages.toObjects(newSchema(), output.pages);
        assertEquals(3, records.size());

        Object[] record;
        Map<Value, Value> map;
        { // "{\"_c0\":true,\"_c1\":10,\"_c2\":\"embulk\",\"_c3\":{\"k\":\"v\"}}"
            record = records.get(0);
            assertEquals(1, record.length);
            map = ((Value) record[0]).asMapValue().map();

            assertEquals(newBoolean(true), map.get(newString("_c0")));
            assertEquals(newInteger(10L), map.get(newString("_c1")));
            assertEquals(newString("embulk"), map.get(newString("_c2")));
            assertEquals(newMap(newString("k"), newString("v")), map.get(newString("_c3")));
        }
        { // "{}"
            record = records.get(1);
            assertEquals(1, record.length);
            assertTrue(((Value) record[0]).asMapValue().map().isEmpty());
        }
        {
            record = records.get(2);
            assertEquals(1, record.length);
            map = ((Value) record[0]).asMapValue().map();

            assertEquals(newBoolean(false), map.get(newString("_c0")));
            assertEquals(newInteger(-10L), map.get(newString("_c1")));
            assertEquals(newString("エンバルク"), map.get(newString("_c2")));
            assertEquals(newArray(newString("e0"), newString("e1")), map.get(newString("_c3")));
        }
    }

    @Test
    public void useStopOnInvalidRecord() throws Exception {
        ConfigSource config = this.config.deepCopy().set("stop_on_invalid_record", true);

        try {
            transaction(config, fileInput(
                    "[1, 2, 3]" // throw DataException
            ));
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof DataException);
        }
    }

    @Test
    public void readBrokenJson() {
        try {
            transaction(config, fileInput(
                    "{\"_c0\":true,\"_c1\":10," // throw DataException
            ));
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof DataException);
        }
    }

    @Test
    public void useDefaultInvalidEscapeStringFunction() throws Exception {
        try {
            transaction(config, fileInput(
                    "{\"\\a\":\"b\"}\\" // throw DataException
            ));
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof DataException);
        }
    }

    @Test
    public void usePassthroughInvalidEscapeStringFunction() throws Exception {
        try {
            ConfigSource config = this.config.deepCopy().set("invalid_string_escapes", "PASSTHROUGH");
            transaction(config, fileInput(
                    "{\"\\a\":\"b\"}\\" // throw DataException
            ));
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof DataException);
        }
    }

    @Test
    public void useSkipInvalidEscapeString() throws Exception {
        ConfigSource config = this.config.deepCopy().set("invalid_string_escapes", "SKIP");
        transaction(config, fileInput(
                "{\"\\a\":\"b\"}\\"
        ));

        List<Object[]> records = Pages.toObjects(newSchema(), output.pages);
        assertEquals(1, records.size());
        Object[] record = records.get(0);
        Map<Value, Value> map = ((Value) record[0]).asMapValue().map();
        assertEquals(newString("b"), map.get(newString("")));
    }

    @Test
    public void useUnEscapeInvalidEscapeString() throws Exception {
        ConfigSource config = this.config.deepCopy().set("invalid_string_escapes", "UNESCAPE");
        transaction(config, fileInput(
                "{\"\\a\":\"b\"}\\"
        ));

        List<Object[]> records = Pages.toObjects(newSchema(), output.pages);
        assertEquals(1, records.size());
        Object[] record = records.get(0);
        Map<Value, Value> map = ((Value) record[0]).asMapValue().map();
        assertEquals(newString("b"), map.get(newString("a")));
    }

    @Test
    public void checkInvalidEscapeStringFunction() {
        //PASSTHROUGH
        {
            String json = "{\\\"_c0\\\":true,\\\"_c1\\\":10,\\\"_c2\\\":\\\"embulk\\\",\\\"_c3\\\":{\\\"k\\\":\\\"v\\\"}}";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(PASSTHROUGH).apply(json);
            assertEquals(json, actual);
        }

        {
            String json = "{\"abc\b\f\n\r\t\\\\u0001\":\"efg\"}\\";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(PASSTHROUGH).apply(json);
            assertEquals(json, actual);
        }

        {
            String json = "{\"\\a\":\"b\"}\\";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(PASSTHROUGH).apply(json);
            assertEquals(json, actual);
        }

        //SKIP
        {
            String json = "{\\\"_c0\\\":true,\\\"_c1\\\":10,\\\"_c2\\\":\\\"embulk\\\",\\\"_c3\\\":{\\\"k\\\":\\\"v\\\"}}";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(SKIP).apply(json);
            assertEquals(json, actual);
        }

        {
            // valid charset u0001
            String json = "{\"abc\b\f\n\r\t\\\\u0001\":\"efg\"}\\";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(SKIP).apply(json);
            assertEquals("{\"abc\b\f\n\r\t\\\\u0001\":\"efg\"}", actual);
        }

        {
            // invalid charset \\u12xY remove forwarding backslash and u
            String json = "{\"\\u12xY\":\"efg\"}\\";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(SKIP).apply(json);
            assertEquals("{\"12xY\":\"efg\"}", actual);
        }

        {
            String json = "{\"\\a\":\"b\"}\\";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(SKIP).apply(json);
            // backslash and `a` will removed.
            assertEquals("{\"\":\"b\"}", actual);
        }

        {
            // end of lines backspash.
            String json = "{\"\\a\":\"b\"}"
                    + "\n"
                    + "\\";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(SKIP).apply(json);
            // backslash and `a` will removed.
            assertEquals("{\"\":\"b\"}\n", actual);
        }

        //UNESCAPE
        {
            String json = "{\\\"_c0\\\":true,\\\"_c1\\\":10,\\\"_c2\\\":\\\"embulk\\\",\\\"_c3\\\":{\\\"k\\\":\\\"v\\\"}}";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(UNESCAPE).apply(json);
            assertEquals(json, actual);
        }

        {
            String json = "{\"abc\b\f\n\r\t\\\\u0001\":\"efg\"}\\";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(UNESCAPE).apply(json);
            assertEquals("{\"abc\b\f\n\r\t\\\\u0001\":\"efg\"}", actual);
        }

        {
            // invalid charset u000x remove forwarding backslash
            String json = "{\"\\u000x\":\"efg\"}\\";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(UNESCAPE).apply(json);
            assertEquals("{\"u000x\":\"efg\"}", actual);
        }

        {
            String json = "{\"\\a\":\"b\"}\\";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(UNESCAPE).apply(json);
            // backslash will removed.
            assertEquals("{\"a\":\"b\"}", actual);
        }

        {
            // end of lines backspash.
            String json = "{\"\\a\":\"b\"}"
                    + "\n"
                    + "\\";
            String actual = JsonParserPlugin.invalidEscapeStringFunction(SKIP).apply(json);
            // backslash and `a` will removed.
            assertEquals("{\"\":\"b\"}\n", actual);
        }
    }

    @Test
    public void useJsonPointerToRoot() throws Exception {
        ConfigSource config = this.config.deepCopy().set("__experimental__json_pointer_to_root", "/_c0");
        transaction(config, fileInput(
                "{\"_c0\":{\"b\": 1}, \"_c1\": true}",
                "{}",            // should be skipped because it doesn't have "_c0"
                "{\"_c0\": 1}",  // should be skipped because the value doesn't map value
                "{\"_c0\":{\"b\": 2}, \"_c1\": false}"
        ));

        List<Object[]> records = Pages.toObjects(newSchema(), output.pages);
        assertEquals(2, records.size());

        Object[] record = records.get(0);
        Map<Value, Value> map = ((Value) record[0]).asMapValue().map();
        assertEquals(newInteger(1), map.get(newString("b")));

        record = records.get(1);
        map = ((Value) record[0]).asMapValue().map();
        assertEquals(newInteger(2), map.get(newString("b")));
    }

    @Test(expected = DataException.class)
    public void useJsonPointerToRootWithStopOnInvalidRecord() throws Exception {
        ConfigSource config = this.config.deepCopy()
                .set("__experimental__json_pointer_to_root", "/_c0")
                .set("stop_on_invalid_record", true);

        transaction(config, fileInput(
                "{\"_c0\":{\"b\": 1}, \"_c1\": true}",
                "{}",            // Stop with the record
                "{\"_c0\":{\"b\": 2}, \"_c1\": false}"
        ));
    }

    @Test
    public void useSchemaConfig() throws Exception {
        // Check parsing all types and inexistent column
        final List<Object> schemaConfig = new ArrayList<>();
        schemaConfig.add(config().set("name", "_c0").set("type", "long"));
        schemaConfig.add(config().set("name", "_c1").set("type", "double"));
        schemaConfig.add(config().set("name", "_c2").set("type", "string"));
        schemaConfig.add(config().set("name", "_c3").set("type", "boolean"));
        schemaConfig.add(config().set("name", "_c4").set("type", "timestamp").set("format", "%Y-%m-%d %H:%M:%S"));
        schemaConfig.add(config().set("name", "_c5").set("type", "json"));
        schemaConfig.add(config().set("name", "_c99").set("type", "string"));

        ConfigSource config = this.config.set("__experimental__columns", schemaConfig);
        transaction(config, fileInput(
                "{\"_c0\": 1, \"_c1\": 1.234, \"_c2\": \"a\", \"_c3\": true, \"_c4\": \"2019-01-02 03:04:56\", \"_c5\":{\"a\": 1}}"
        ));

        List<Object[]> records = Pages.toObjects(newSchema(), output.pages);
        assertEquals(1, records.size());

        Object[] record = records.get(0);
        assertArrayEquals(record, new Object[]{1L, 1.234D, "a", true, toTimestamp("2019-01-02 03:04:56"), toJson("{\"a\": 1}"), null});
    }

    @Test
    public void useSchemaConfigWithJsonPointer() throws Exception {
        // Check parsing all types and inexistent column
        final List<Object> schemaConfig = new ArrayList<>();
        schemaConfig.add(config().set("name", "_c0").set("type", "long").set("relative_json_pointer_from_root", "/a/0"));
        schemaConfig.add(config().set("name", "_c1").set("type", "double").set("relative_json_pointer_from_root", "/a/1"));
        schemaConfig.add(config().set("name", "_c2").set("type", "string").set("relative_json_pointer_from_root", "/a/2"));
        schemaConfig.add(config().set("name", "_c3").set("type", "boolean").set("relative_json_pointer_from_root", "/a/3/b/0"));
        schemaConfig.add(config().set("name", "_c4").set("type", "timestamp").set("format", "%Y-%m-%d %H:%M:%S").set("relative_json_pointer_from_root", "/a/3/b/1"));
        schemaConfig.add(config().set("name", "_c5").set("type", "json").set("relative_json_pointer_from_root", "/c"));
        schemaConfig.add(config().set("name", "_c99").set("type", "json").set("relative_json_pointer_from_root", "/d"));

        ConfigSource config = this.config.set("__experimental__columns", schemaConfig);
        transaction(config, fileInput(
                "{\"a\": [1, 1.234, \"foo\", {\"b\": [true, \"2019-01-02 03:04:56\"]}], \"c\": {\"a\": 1}}"
        ));

        List<Object[]> records = Pages.toObjects(newSchema(), output.pages);
        assertEquals(1, records.size());

        Object[] record = records.get(0);
        assertArrayEquals(record, new Object[]{1L, 1.234D, "foo", true, toTimestamp("2019-01-02 03:04:56"), toJson("{\"a\": 1}"), null});
    }

    @Test
    public void useFlattenJsonArray() throws Exception {
        ConfigSource config = this.config.set("__experimental__flatten_json_array", true);
        transaction(config, fileInput(
                "[{\"_c0\": 1},{\"_c0\": 2}]"
        ));

        List<Object[]> records = Pages.toObjects(newSchema(), output.pages);
        assertEquals(2, records.size());
        assertArrayEquals(records.get(0), new Object[]{toJson("{\"_c0\": 1}")});
        assertArrayEquals(records.get(1), new Object[]{toJson("{\"_c0\": 2}")});
    }

    @Test(expected = DataException.class)
    public void useFlattenJsonArrayWithNonArrayJson() throws Exception {
        ConfigSource config = this.config
                .set("__experimental__flatten_json_array", true)
                .set("stop_on_invalid_record", true);

        transaction(config, fileInput(
                "{\"_c0\": 1}"
        ));
    }

    @Test
    public void useFlattenJsonArrayWithRootPointer() throws Exception {
        ConfigSource config = this.config
                .set("__experimental__flatten_json_array", true)
                .set("__experimental__json_pointer_to_root", "/a");
        transaction(config, fileInput(
                "{\"a\": [{\"_c0\": 1},{\"_c0\": 2}]}"
        ));

        List<Object[]> records = Pages.toObjects(newSchema(), output.pages);
        assertEquals(2, records.size());
        assertArrayEquals(records.get(0), new Object[]{toJson("{\"_c0\": 1}")});
        assertArrayEquals(records.get(1), new Object[]{toJson("{\"_c0\": 2}")});
    }

    private ConfigSource config() {
        return runtime.getExec().newConfigSource();
    }

    private void transaction(ConfigSource config, final FileInput input) {
        plugin.transaction(config, (taskSource, schema) -> plugin.run(taskSource, schema, input, output));
    }

    private FileInput fileInput(String... lines) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }

        ByteArrayInputStream in = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
        return new InputStreamFileInput(runtime.getBufferAllocator(), provider(in));
    }

    private InputStreamFileInput.IteratorProvider provider(InputStream... inputStreams) throws IOException {
        return new InputStreamFileInput.IteratorProvider(
                ImmutableList.copyOf(inputStreams));
    }

    private Schema newSchema() {
        return plugin.newSchema(config.loadConfig(JsonParserPlugin.PluginTask.class));
    }

    private static Timestamp toTimestamp(String dateTimeString) {
        return TIMESTAMP_PARSER.parse(dateTimeString);
    }

    private static Value toJson(String json) {
        return JSON_PARSER.parse(json);
    }

    private static final TimestampParser TIMESTAMP_PARSER = TimestampParser.of("java:yyyy-MM-dd HH:mm:ss", "UTC");

    private static final JsonParser JSON_PARSER = new JsonParser();
}
