package org.embulk.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.embulk.config.ModelManager;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.Types;
import org.embulk.test.EmbulkTestRuntime;
import org.junit.Rule;
import org.junit.Test;

public class TestColumnMapping {
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Test
    public void testSerialization() throws IOException {
        final ModelManager modelManager = ExecInternal.getModelManager();

        final Column column = new Column(21, "bar", Types.DOUBLE);
        final String stringified = modelManager.writeObject(column);
        final JsonNode reparsed = (new ObjectMapper()).readTree(stringified);
        assertTrue(reparsed.get("index").isInt());
        assertEquals(21, reparsed.get("index").intValue());
        assertTrue(reparsed.get("name").isTextual());
        assertEquals("bar", reparsed.get("name").textValue());
        assertTrue(reparsed.get("type").isTextual());
        assertEquals("double", reparsed.get("type").textValue());
    }

    @Test
    public void testNormal() {
        assertColumn(12, "foo", Types.LONG, "12", "\"foo\"", "\"long\"");
    }

    @Test
    public void testStringIndex() {
        assertColumn(9491, "foo", Types.LONG, "\"9491\"", "\"foo\"", "\"long\"");
    }

    @Test
    public void testDoubleIndex1() {
        assertColumn(481, "foo", Types.LONG, "481.4", "\"foo\"", "\"long\"");
    }

    @Test
    public void testDoubleIndex2() {
        assertColumn(481, "foo", Types.LONG, "481.6", "\"foo\"", "\"long\"");
    }

    @Test
    public void testDoubleStringIndex() {
        assertMappingException("\"1938.45\"", "\"foo\"", "\"long\"");
    }

    @Test
    public void testNullIndex() {
        assertMappingException("null", "\"foo\"", "\"long\"");
    }

    @Test
    public void testArrayIndex() {
        assertMappingException("[\"bar\"]", "\"foo\"", "\"long\"");
    }

    @Test
    public void testObjectIndex() {
        assertMappingException("{\"bar\":\"baz\"}", "\"foo\"", "\"long\"");
    }

    @Test
    public void testTrueIndex() {
        assertMappingException("true", "\"foo\"", "\"long\"");
    }

    @Test
    public void testFalseIndex() {
        assertMappingException("false", "\"foo\"", "\"long\"");
    }

    @Test
    public void testTrueStringIndex() {
        assertMappingException("\"true\"", "\"foo\"", "\"long\"");
    }

    @Test
    public void testFalseStringIndex() {
        assertMappingException("\"false\"", "\"foo\"", "\"long\"");
    }

    @Test
    public void testZeroName() {
        assertColumn(12, "0", Types.LONG, "12", "0", "\"long\"");
    }

    @Test
    public void testIntegralName() {
        assertColumn(12, "49814", Types.LONG, "12", "49814", "\"long\"");
    }

    @Test
    public void testZeroLeadingIntegralName() {
        assertException(JsonParseException.class, "12", "049814", "\"long\"");
    }

    @Test
    public void testDoubleName() {
        assertColumn(12, "49814.12034", Types.LONG, "12", "49814.12034", "\"long\"");
    }

    @Test
    public void testTrueName() {
        assertColumn(12, "true", Types.LONG, "12", "true", "\"long\"");
    }

    @Test
    public void testFalseName() {
        assertColumn(12, "false", Types.LONG, "12", "false", "\"long\"");
    }

    @Test
    public void testNumericalName() {
        assertColumn(12, "123", Types.LONG, "12", "\"123\"", "\"long\"");
    }

    @Test
    public void testSymbolicName() {
        assertColumn(12, "###", Types.LONG, "12", "\"###\"", "\"long\"");
    }

    @Test
    public void testArrayName() {
        assertMappingException("12", "[\"foo\"]", "\"long\"");
    }

    @Test
    public void testObjectName() {
        assertMappingException("12", "{\"foo\":\"bar\"}", "\"long\"");
    }

    @Test
    public void testNullName() {
        assertMappingException("12", "null", "\"long\"");
    }

    @Test
    public void testDoubleType() {
        assertColumn(12, "foo", Types.DOUBLE, "12", "\"foo\"", "\"double\"");
    }

    @Test
    public void testBooleanType() {
        assertColumn(12, "foo", Types.BOOLEAN, "12", "\"foo\"", "\"boolean\"");
    }

    @Test
    public void testJsonType() {
        assertColumn(12, "foo", Types.JSON, "12", "\"foo\"", "\"json\"");
    }

    @Test
    public void testStringType() {
        assertColumn(12, "foo", Types.STRING, "12", "\"foo\"", "\"string\"");
    }

    @Test
    public void testTimestampType() {
        assertColumn(12, "foo", Types.TIMESTAMP, "12", "\"foo\"", "\"timestamp\"");
    }

    @Test
    public void testInvalidStringType() {
        assertMappingException("12", "\"foo\"", "\"invalid\"");
    }

    @Test
    public void testNumericType() {
        assertMappingException("12", "\"foo\"", "931");
    }

    @Test
    public void testNullType() {
        assertMappingException("12", "\"foo\"", "null");
    }

    @Test
    public void testArrayType() {
        assertMappingException("12", "\"foo\"", "[\"baz\"]");
    }

    @Test
    public void testObjectType() {
        assertMappingException("12", "\"foo\"", "{\"baz\":\"bar\"}");
    }

    @Test
    public void testNoIndex() {
        assertMappingException("{\"name\":\"foo\",\"type\":\"long\"}");
    }

    @Test
    public void testNoName() {
        assertMappingException("{\"index\":12,\"type\":\"long\"}");
    }

    @Test
    public void testNoType() {
        assertMappingException("{\"index\":12,\"name\":\"foo\"}");
    }

    @Test
    public void testArray() {
        assertMappingException("[\"index\", 12, \"name\", \"foo\", \"type\", \"long\"]");
    }

    @Test
    public void testDirectValue() {
        assertMappingException("123");
    }

    @Test
    public void testEmpty() {
        assertMappingException("");
    }

    @Test
    public void testNull() {
        assertMappingException("null");
    }

    private static void assertColumn(
            final int indexExpected, final String nameExpected, final Type typeExpected, final String json) {
        final ModelManager modelManager = ExecInternal.getModelManager();
        final Column column = modelManager.readObject(Column.class, json);
        assertEquals(indexExpected, column.getIndex());
        assertEquals(nameExpected, column.getName());
        assertEquals(typeExpected, column.getType());
    }

    private static void assertColumn(
            final int indexExpected, final String nameExpected, final Type typeExpected,
            final String indexValue, final String nameValue, final String typeValue) {
        assertColumn(indexExpected, nameExpected, typeExpected,
                     String.format("{\"index\":%s,\"name\":%s,\"type\":%s}", indexValue, nameValue, typeValue));
    }

    private static void assertException(final Class<? extends Exception> expectedException, final String json) {
        final ModelManager modelManager = ExecInternal.getModelManager();
        try {
            modelManager.readObject(Column.class, json);
        } catch (final RuntimeException ex) {
            final Throwable cause = ex.getCause();
            if (!(expectedException.isInstance(cause))) {
                cause.printStackTrace();
                fail();
            }
        }
    }

    private static void assertException(
            final Class<? extends Exception> expectedException,
            final String indexValue, final String nameValue, final String typeValue) {
        assertException(
                expectedException, String.format("{\"index\":%s,\"name\":%s,\"type\":%s}", indexValue, nameValue, typeValue));
    }

    private static void assertMappingException(final String json) {
        assertException(JsonMappingException.class, json);
    }

    private static void assertMappingException(final String indexValue, final String nameValue, final String typeValue) {
        assertMappingException(String.format("{\"index\":%s,\"name\":%s,\"type\":%s}", indexValue, nameValue, typeValue));
    }
}
