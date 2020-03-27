package org.embulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Properties;
import org.junit.Test;

public class TestEmbulkSystemProperties {
    @Test
    public void testParseInteger() {
        assertInteger("11", 11, 1234);
        assertInteger("  13", 13, 1234);
        assertInteger("  17  ", 17, 1234);
        assertInteger("111111111", 111111111, 1234);
        assertInteger("-11", -11, 1234);
    }

    @Test
    public void testParseIntegerInvalidFormat() {
        /*
         * They failed originally like below with Jackson:
         *
         * "fuo"
         *   com.fasterxml.jackson.databind.exc.InvalidFormatException:
         *   Can not construct instance of java.lang.Integer from String value 'fuo': not a valid Integer value
         * "true"
         *   com.fasterxml.jackson.databind.exc.InvalidFormatException
         *   Can not construct instance of java.lang.Integer from String value 'true': not a valid Integer value
         * "11f"
         *   com.fasterxml.jackson.databind.exc.InvalidFormatException
         *   Can not construct instance of java.lang.Integer from String value '11f': not a valid Integer value
         * "11.131"
         *   com.fasterxml.jackson.databind.exc.InvalidFormatException
         *   Can not construct instance of java.lang.Integer from String value '11.131': not a valid Integer value
         * "111111111111"
         *   com.fasterxml.jackson.databind.exc.InvalidFormatException:
         *   Can not construct instance of java.lang.Integer from String value '111111111111':
         *   Overflow: numeric value (111111111111) out of range of Integer (-2147483648 - 2147483647)
         */
        assertIntegerInvalidFormat("fuo");
        assertIntegerInvalidFormat("true");
        assertIntegerInvalidFormat("True");
        assertIntegerInvalidFormat("false");
        assertIntegerInvalidFormat("11f");
        assertIntegerInvalidFormat("11.131");
        assertIntegerInvalidFormat("111111111111");
    }

    @Test
    public void testParseIntegerNull() {
        assertIntegerNull("null");
        assertIntegerNull("");
        assertIntegerNull("  ");
    }

    @Test
    public void testParseBoolean() {
        assertBoolean("true", true, false);
        assertBoolean("True", true, false);
        assertBoolean("false", false, false);
        assertBoolean("false", false, false);

        assertBoolean("  true", true, true);
        assertBoolean("  True", true, true);
        assertBoolean(" false  ", false, true);
        assertBoolean(" false  ", false, true);

        assertBoolean("", false, false);
        assertBoolean("", true, true);
        assertBoolean("  ", false, false);
        assertBoolean("  ", true, true);
        assertBoolean("null", false, false);
        assertBoolean("null", true, true);
        assertBoolean(" null  ", false, false);
        assertBoolean(" null  ", true, true);
    }

    @Test
    public void testParseBooleanInvalidFormat() {
        /*
         * They failed originally like below with Jackson:
         *
         * "tRuE"
         *   com.fasterxml.jackson.databind.exc.InvalidFormatException:
         *   Can not construct instance of boolean from String value 'tRuE': only "true" or "false" recognized
         * "TRUE"
         *   com.fasterxml.jackson.databind.exc.InvalidFormatException:
         *   Can not construct instance of boolean from String value 'TRUE': only "true" or "false" recognized
         * "0"
         *   com.fasterxml.jackson.databind.exc.InvalidFormatException:
         *   Can not construct instance of boolean from String value '0': only "true" or "false" recognized
         * "1"
         *   com.fasterxml.jackson.databind.exc.InvalidFormatException:
         *   Can not construct instance of boolean from String value '1': only "true" or "false" recognized
         */
        assertBooleanInvalidFormat("TRUE");
        assertBooleanInvalidFormat("tRuE");
        assertBooleanInvalidFormat("FALSE");
        assertBooleanInvalidFormat("falsE");
        assertBooleanInvalidFormat("0");
        assertBooleanInvalidFormat("1");
        assertBooleanInvalidFormat("123456");
        assertBooleanInvalidFormat("foo");
    }

    private static void assertInteger(final String text, final int expectedValue, final int defaultValue) {
        final Properties properties = new Properties();
        properties.setProperty("key", text);
        assertEquals(expectedValue, EmbulkSystemProperties.of(properties).getPropertyAsInteger("key", defaultValue));
    }

    private static void assertIntegerInvalidFormat(final String text) {
        final Properties properties = new Properties();
        properties.setProperty("key", text);
        try {
            final int unexpectedResult = EmbulkSystemProperties.of(properties).getPropertyAsInteger("key", 123456);
            fail("\"" + text + "\" was unexpectedly parsed successfully: " + unexpectedResult);
        } catch (final IllegalArgumentException ex) {
            return;  // Success.
        }
    }

    private static void assertIntegerNull(final String text) {
        final Properties properties = new Properties();
        properties.setProperty("key", text);
        try {
            final int unexpectedResult = EmbulkSystemProperties.of(properties).getPropertyAsInteger("key", 123456);
            fail("\"" + text + "\" was unexpectedly parsed successfully: " + unexpectedResult);
        } catch (final NullPointerException ex) {
            return;  // Success.
        }
    }

    private static void assertBoolean(final String text, final boolean expectedValue, final boolean defaultValue) {
        final Properties properties = new Properties();
        properties.setProperty("key", text);
        assertEquals(expectedValue, EmbulkSystemProperties.of(properties).getPropertyAsBoolean("key", defaultValue));
    }

    private static void assertBooleanInvalidFormat(final String text) {
        final Properties properties = new Properties();
        properties.setProperty("key", text);
        try {
            final boolean unexpectedResult = EmbulkSystemProperties.of(properties).getPropertyAsBoolean("key", false);
            fail("\"" + text + "\" was unexpectedly parsed successfully: " + unexpectedResult);
        } catch (final IllegalArgumentException ex) {
            return;  // Success.
        }
    }
}
