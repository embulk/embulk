package org.embulk.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.IOException;
import org.embulk.spi.unit.ToString;
import org.junit.Test;

public class TestToString {
    @Test
    public void test() throws IOException {
        assertToString("000abc", "\"000abc\"");
        assertNull("null");
        assertToString("null", "\"null\"");
        assertToString("12", "12");
        assertToString("true", "true");

        assertMappingException("{\"foo\": \"000abc\", \"bar\": null, \"baz\": 12, \"qux\": true}");
        assertMappingException("[ \"something\" ]");
    }

    @SuppressWarnings("deprecation")
    private static void assertMappingException(final String inputJson) throws IOException {
        try {
            MAPPER.readValue(inputJson, ToString.class);
        } catch (final JsonMappingException ex) {
            return;
        }
        fail("JsonMappingException is expected.");
    }

    @SuppressWarnings("deprecation")
    private static void assertToString(final String expected, final String inputJson) throws IOException {
        final ToString toString = MAPPER.readValue(inputJson, ToString.class);
        assertEquals(expected, toString.toString());
    }

    @SuppressWarnings("deprecation")
    private static void assertNull(final String inputJson) throws IOException {
        final ToString toString = MAPPER.readValue(inputJson, ToString.class);
        assertEquals(null, toString);
    }

    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new Jdk8Module());
        registerToStringJacksonModule(MAPPER);
    }

    @SuppressWarnings("deprecation")
    private static void registerToStringJacksonModule(final ObjectMapper mapper) {
        mapper.registerModule(new ToStringJacksonModule());
    }
}
