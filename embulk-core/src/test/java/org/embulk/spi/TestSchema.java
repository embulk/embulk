package org.embulk.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ModelManager;
import org.embulk.spi.Schema;
import org.embulk.spi.type.Types;
import org.junit.Rule;
import org.junit.Test;

public class TestSchema {
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Test
    public void testSerialization() throws IOException {
        final ModelManager modelManager = Exec.getModelManager();
        final Schema schema = Schema.builder().add("hoge", Types.DOUBLE).add("fuga", Types.JSON).build();
        final String stringified = modelManager.writeObject(schema);
        final JsonNode reparsed = (new ObjectMapper()).readTree(stringified);
        assertTrue(reparsed.isArray());
        final ArrayNode array = (ArrayNode) reparsed;
        assertEquals(2, array.size());
        final JsonNode object1 = array.get(0);
        assertTrue(object1.isObject());
        final JsonNode object2 = array.get(1);
        assertTrue(object2.isObject());

        final ObjectNode column1 = (ObjectNode) object1;
        assertTrue(column1.get("index").isInt());
        assertEquals(0, column1.get("index").intValue());
        assertTrue(column1.get("name").isTextual());
        assertEquals("hoge", column1.get("name").textValue());
        assertTrue(column1.get("type").isTextual());
        assertEquals("double", column1.get("type").textValue());

        final ObjectNode column2 = (ObjectNode) object2;
        assertTrue(column2.get("index").isInt());
        assertEquals(1, column2.get("index").intValue());
        assertTrue(column2.get("name").isTextual());
        assertEquals("fuga", column2.get("name").textValue());
        assertTrue(column2.get("type").isTextual());
        assertEquals("json", column2.get("type").textValue());
    }

    @Test
    public void testEmpty() {
        final Schema schema = readSchema("[]");
        assertEquals(0, schema.getColumns().size());
        assertEquals(0, schema.size());
        assertEquals(0, schema.getColumnCount());
        assertTrue(schema.isEmpty());
    }

    @Test
    public void testOne() {
        final Schema schema = readSchema("[{\"index\":0,\"name\":\"foo\",\"type\":\"long\"}]");

        final List<Column> columns = schema.getColumns();
        assertEquals(1, columns.size());
        assertEquals(0, columns.get(0).getIndex());
        assertEquals("foo", columns.get(0).getName());
        assertEquals(Types.LONG, columns.get(0).getType());

        assertEquals(1, schema.size());
        assertEquals(1, schema.getColumnCount());

        assertEquals(0, schema.getColumn(0).getIndex());
        assertEquals("foo", schema.getColumn(0).getName());
        assertEquals(Types.LONG, schema.getColumn(0).getType());

        assertEquals("foo", schema.getColumnName(0));
        assertEquals(Types.LONG, schema.getColumnType(0));

        schema.visitColumns(new ColumnVisitor() {
                @Override
                public void booleanColumn(Column column) {
                    fail();
                }

                @Override
                public void longColumn(Column column) {
                    assertEquals(0, column.getIndex());
                    assertEquals("foo", column.getName());
                    assertEquals(Types.LONG, column.getType());
                }

                @Override
                public void doubleColumn(Column column) {
                    fail();
                }

                @Override
                public void stringColumn(Column column) {
                    fail();
                }

                @Override
                public void timestampColumn(Column column) {
                    fail();
                }

                @Override
                public void jsonColumn(Column column) {
                    fail();
                }
            });

        assertFalse(schema.isEmpty());

        assertEquals(columns.get(0), schema.lookupColumn("foo"));
        assertSchemaConfigExceptionForLookupColumn(schema, "bar");

        assertEquals(8, schema.getFixedStorageSize());
    }

    private static void assertSchemaConfigExceptionForLookupColumn(final Schema schema, final String unknown) {
        try {
            schema.lookupColumn(unknown);
        } catch (final SchemaConfigException expected) {
            return;
        }
        fail();
    }

    private static Schema readSchema(final String json) {
        final ModelManager modelManager = Exec.getModelManager();
        return modelManager.readObject(Schema.class, json);
    }
}
