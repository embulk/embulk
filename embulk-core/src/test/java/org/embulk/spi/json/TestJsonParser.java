package org.embulk.spi.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.msgpack.value.Value;

public class TestJsonParser {
    @Test
    public void testString() throws Exception {
        final JsonParser parser = new JsonParser();
        final Value msgpackValue = parser.parse("\"foobar\"");
        assertFalse(msgpackValue.getValueType().isNumberType());
        assertTrue(msgpackValue.getValueType().isStringType());
        assertEquals("foobar", msgpackValue.asStringValue().toString());
    }

    @Test(expected = JsonParseException.class)
    public void testStringUnquoted() throws Exception {
        final JsonParser parser = new JsonParser();
        parser.parse("foobar");
    }

    @Test
    public void testOrdinaryInteger() throws Exception {
        final JsonParser parser = new JsonParser();
        final Value msgpackValue = parser.parse("12345");
        assertTrue(msgpackValue.getValueType().isNumberType());
        assertTrue(msgpackValue.getValueType().isIntegerType());
        assertFalse(msgpackValue.getValueType().isFloatType());
        assertFalse(msgpackValue.getValueType().isStringType());
        assertEquals(12345, msgpackValue.asIntegerValue().asInt());
    }

    @Test
    public void testExponentialInteger1() throws Exception {
        final JsonParser parser = new JsonParser();
        final Value msgpackValue = parser.parse("12345e3");
        assertTrue(msgpackValue.getValueType().isNumberType());
        // TODO: Consider this needs to be an integer?
        // See: https://github.com/embulk/embulk/issues/775
        assertTrue(msgpackValue.getValueType().isFloatType());
        assertFalse(msgpackValue.getValueType().isIntegerType());
        assertFalse(msgpackValue.getValueType().isStringType());
        assertEquals(12345000.0, msgpackValue.asFloatValue().toDouble(), 0.000000001);
        // Not sure this |toString| is to be tested...
        assertEquals("1.2345E7", msgpackValue.asFloatValue().toString());
    }

    @Test
    public void testExponentialInteger2() throws Exception {
        final JsonParser parser = new JsonParser();
        final Value msgpackValue = parser.parse("123e2");
        assertTrue(msgpackValue.getValueType().isNumberType());
        // TODO: Consider this needs to be an integer?
        // See: https://github.com/embulk/embulk/issues/775
        assertTrue(msgpackValue.getValueType().isFloatType());
        assertFalse(msgpackValue.getValueType().isIntegerType());
        assertFalse(msgpackValue.getValueType().isStringType());
        assertEquals(12300.0, msgpackValue.asFloatValue().toDouble(), 0.000000001);
        // Not sure this |toString| is to be tested...
        assertEquals("12300.0", msgpackValue.asFloatValue().toString());
    }

    @Test
    public void testOrdinaryFloat() throws Exception {
        final JsonParser parser = new JsonParser();
        final Value msgpackValue = parser.parse("12345.12");
        assertTrue(msgpackValue.getValueType().isNumberType());
        assertTrue(msgpackValue.getValueType().isFloatType());
        assertFalse(msgpackValue.getValueType().isIntegerType());
        assertFalse(msgpackValue.getValueType().isStringType());
        assertEquals(12345.12, msgpackValue.asFloatValue().toDouble(), 0.000000001);
        // Not sure this |toString| is to be tested...
        assertEquals("12345.12", msgpackValue.asFloatValue().toString());
    }

    @Test
    public void testExponentialFloat() throws Exception {
        final JsonParser parser = new JsonParser();
        final Value msgpackValue = parser.parse("1.234512E4");
        assertTrue(msgpackValue.getValueType().isNumberType());
        assertTrue(msgpackValue.getValueType().isFloatType());
        assertFalse(msgpackValue.getValueType().isIntegerType());
        assertFalse(msgpackValue.getValueType().isStringType());
        assertEquals(12345.12, msgpackValue.asFloatValue().toDouble(), 0.000000001);
        // Not sure this |toString| is to be tested...
        assertEquals("12345.12", msgpackValue.asFloatValue().toString());
    }
}
