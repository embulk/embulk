package org.embulk.spi.json;

import org.junit.Test;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestJsonParser
{
    @Test
    public void testString() throws Exception
    {
        final JsonParser parser = new JsonParser();
        final Value msgpackValue = parser.parse("\"foobar\"");
        assertFalse(msgpackValue.getValueType().isNumberType());
        assertTrue(msgpackValue.getValueType().isStringType());
    }

    @Test(expected = JsonParseException.class)
    public void testStringUnquoted() throws Exception
    {
        final JsonParser parser = new JsonParser();
        parser.parse("foobar");
    }

    @Test
    public void testOrdinaryInteger() throws Exception
    {
        final JsonParser parser = new JsonParser();
        final Value msgpackValue = parser.parse("12345");
        assertTrue(msgpackValue.getValueType().isNumberType());
        assertTrue(msgpackValue.getValueType().isIntegerType());
        assertFalse(msgpackValue.getValueType().isFloatType());
        assertFalse(msgpackValue.getValueType().isStringType());
    }

    @Test
    public void testExponentialInteger() throws Exception
    {
        final JsonParser parser = new JsonParser();
        System.out.println("12345e3");
        final Value msgpackValue = parser.parse("12345e3");
        System.out.println("Done: 12345e3");
        assertTrue(msgpackValue.getValueType().isNumberType());
        // TODO: Should be an integer!
        assertTrue(msgpackValue.getValueType().isFloatType());
        assertFalse(msgpackValue.getValueType().isIntegerType());
        assertFalse(msgpackValue.getValueType().isStringType());
    }

    @Test
    public void testOrdinaryFloat() throws Exception
    {
        final JsonParser parser = new JsonParser();
        final Value msgpackValue = parser.parse("12345.12");
        assertTrue(msgpackValue.getValueType().isNumberType());
        assertTrue(msgpackValue.getValueType().isFloatType());
        assertFalse(msgpackValue.getValueType().isIntegerType());
        assertFalse(msgpackValue.getValueType().isStringType());
    }

    @Test
    public void testExponentialFloat() throws Exception
    {
        final JsonParser parser = new JsonParser();
        final Value msgpackValue = parser.parse("1.12e3");
        assertTrue(msgpackValue.getValueType().isNumberType());
        assertTrue(msgpackValue.getValueType().isFloatType());
        assertFalse(msgpackValue.getValueType().isIntegerType());
        assertFalse(msgpackValue.getValueType().isStringType());
    }
}
