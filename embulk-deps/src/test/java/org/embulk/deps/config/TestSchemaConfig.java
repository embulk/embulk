package org.embulk.deps.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.embulk.config.ConfigSource;
import org.embulk.config.ModelManager;
import org.embulk.spi.ColumnConfig;
import org.embulk.spi.ExecInternal;
import org.embulk.spi.SchemaConfig;
import org.embulk.spi.type.Types;
import org.embulk.test.EmbulkTestRuntime;
import org.junit.Rule;
import org.junit.Test;

public class TestSchemaConfig {
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Test
    public void testColumnConfigSerDe1() throws IOException {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("name", "foo");
        node.put("type", "boolean");

        final ModelManager model = ExecInternal.getModelManager();
        final ColumnConfig config = model.readObject(ColumnConfig.class, MAPPER.writeValueAsString(node));
        assertEquals("foo", config.getName());
        assertEquals(Types.BOOLEAN, config.getType());
        assertTrue(config.getOption().isEmpty());

        assertEquals("{\"name\":\"foo\",\"type\":\"boolean\"}", model.writeObject(config));
    }

    @Test
    public void testColumnConfigSerDe2() throws IOException {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("name", "barbar");
        node.put("type", "double");
        node.put("format", "%z");

        final ModelManager model = ExecInternal.getModelManager();
        final ColumnConfig config = model.readObject(ColumnConfig.class, MAPPER.writeValueAsString(node));
        assertEquals("barbar", config.getName());
        assertEquals(Types.DOUBLE, config.getType());
        final ConfigSource option = config.getOption();
        assertEquals(1, option.getAttributeNames().size());
        assertEquals("%z", option.get(String.class, "format"));

        assertEquals("{\"format\":\"%z\",\"name\":\"barbar\",\"type\":\"double\"}", model.writeObject(config));
    }

    @Test
    public void testColumnConfigSerDe3() throws IOException {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("name", "bazqux");
        node.put("type", "json");
        node.put("format", "%H");
        node.put("dummy", "hoge");

        final ModelManager model = ExecInternal.getModelManager();
        final ColumnConfig config = model.readObject(ColumnConfig.class, MAPPER.writeValueAsString(node));
        assertEquals("bazqux", config.getName());
        assertEquals(Types.JSON, config.getType());
        final ConfigSource option = config.getOption();
        assertEquals(2, option.getAttributeNames().size());
        assertEquals("%H", option.get(String.class, "format"));
        assertEquals("hoge", option.get(String.class, "dummy"));

        assertEquals("{\"format\":\"%H\",\"dummy\":\"hoge\",\"name\":\"bazqux\",\"type\":\"json\"}", model.writeObject(config));
    }

    @Test
    public void testColumnConfigDeserizalizeToFail1() throws IOException {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("name", "foo");

        final ModelManager model = ExecInternal.getModelManager();
        try {
            model.readObject(ColumnConfig.class, MAPPER.writeValueAsString(node));
        } catch (final RuntimeException ex) {
            assertTrue(ex.getCause() instanceof JsonMappingException);
            return;
        }
        fail();
    }

    @Test
    public void testColumnConfigDeserizalizeToFail2() throws IOException {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "string");

        final ModelManager model = ExecInternal.getModelManager();
        try {
            model.readObject(ColumnConfig.class, MAPPER.writeValueAsString(node));
        } catch (final RuntimeException ex) {
            assertTrue(ex.getCause() instanceof JsonMappingException);
            return;
        }
        fail();
    }

    @Test
    public void testColumnConfigDeserizalizeToFail3() throws IOException {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("name", "foo");
        node.put("type", "invalid_type");

        final ModelManager model = ExecInternal.getModelManager();
        try {
            model.readObject(ColumnConfig.class, MAPPER.writeValueAsString(node));
        } catch (final RuntimeException ex) {
            assertTrue(ex.getCause() instanceof JsonMappingException);
            return;
        }
        fail();
    }

    @Test
    public void testSchemaConfigSerDe0() throws IOException {
        final ArrayNode array = MAPPER.createArrayNode();

        final ModelManager model = ExecInternal.getModelManager();
        final SchemaConfig schema = model.readObject(SchemaConfig.class, MAPPER.writeValueAsString(array));
        assertEquals(0, schema.size());

        assertEquals("[]", model.writeObject(schema));
    }

    @Test
    public void testSchemaConfigSerDe1() throws IOException {
        final ObjectNode node1 = MAPPER.createObjectNode();
        node1.put("name", "foo1");
        node1.put("type", "string");
        final ObjectNode node2 = MAPPER.createObjectNode();
        node2.put("name", "bar2");
        node2.put("type", "timestamp");
        node2.put("format", "%H%M%S");
        final ObjectNode node3 = MAPPER.createObjectNode();
        node3.put("name", "baz3");
        node3.put("type", "long");
        node3.put("dummy", "something");
        final ArrayNode array = MAPPER.createArrayNode();
        array.add(node1);
        array.add(node2);
        array.add(node3);

        final ModelManager model = ExecInternal.getModelManager();
        final SchemaConfig schema = model.readObject(SchemaConfig.class, MAPPER.writeValueAsString(array));
        assertEquals(3, schema.size());

        final ColumnConfig column1 = schema.getColumn(0);
        assertEquals("foo1", column1.getName());
        assertEquals(Types.STRING, column1.getType());
        assertTrue(column1.getOption().isEmpty());

        final ColumnConfig column2 = schema.getColumn(1);
        assertEquals("bar2", column2.getName());
        assertEquals(Types.TIMESTAMP, column2.getType());
        final ConfigSource option2 = column2.getOption();
        assertEquals(1, option2.getAttributeNames().size());
        assertEquals("%H%M%S", option2.get(String.class, "format"));

        final ColumnConfig column3 = schema.getColumn(2);
        assertEquals("baz3", column3.getName());
        assertEquals(Types.LONG, column3.getType());
        final ConfigSource option3 = column3.getOption();
        assertEquals(1, option3.getAttributeNames().size());
        assertEquals("something", option3.get(String.class, "dummy"));

        assertEquals(
                "[{\"name\":\"foo1\",\"type\":\"string\"},{\"format\":\"%H%M%S\",\"name\":\"bar2\",\"type\":\"timestamp\"},"
                + "{\"dummy\":\"something\",\"name\":\"baz3\",\"type\":\"long\"}]",
                model.writeObject(schema));
    }

    @Test
    public void testSchemaConfigDeserializeToFail() throws IOException {
        final ObjectNode node1 = MAPPER.createObjectNode();
        node1.put("name", "foo1");
        node1.put("type", "string");
        final ObjectNode node2 = MAPPER.createObjectNode();
        node2.put("name", "bar2");
        node2.put("type", "timestamp");
        node2.put("format", "%H%M%S");
        final ObjectNode node3 = MAPPER.createObjectNode();
        node3.put("name", "baz3");
        node3.put("type", "invalid_type");
        node3.put("dummy", "something");
        final ArrayNode array = MAPPER.createArrayNode();
        array.add(node1);
        array.add(node2);
        array.add(node3);

        final ModelManager model = ExecInternal.getModelManager();
        try {
            model.readObject(SchemaConfig.class, MAPPER.writeValueAsString(array));
        } catch (final RuntimeException ex) {
            assertTrue(ex.getCause() instanceof JsonMappingException);
            return;
        }
        fail();
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
}
