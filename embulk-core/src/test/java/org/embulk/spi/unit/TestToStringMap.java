package org.embulk.spi.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class TestToStringMap {
    @Test
    public void test() throws IOException {
        assertMappingException("\"foo\"");

        final HashMap<String, String> m1 = new HashMap<>();
        m1.put("foo", "000abc");
        m1.put("bar", "null");
        m1.put("baz", "12");
        m1.put("qux", "true");
        assertToStringMap(m1, "{\"foo\": \"000abc\", \"bar\": null, \"baz\": 12, \"qux\": true}");

        // Handling of null (JSON null) is different between ToStringMap and Map<String, String>.
        // ToStringMap:        maps into "null".
        // Map<String, String: maps into null.
        assertWithMap("{\"foo\": \"000abc\", \"baz\": 12, \"qux\": true}");

        assertMappingException("{\"foo\": [ \"something\" ]}");
        assertMappingException("{\"foo\": { \"some\": \"thing\" }}");
        assertMappingException("\"foo\": 000123");
    }

    private static void assertMappingException(final String inputJson) throws IOException {
        try {
            MAPPER.readValue(inputJson, ToStringMap.class);
        } catch (final JsonMappingException ex) {
            return;
        }
        fail("JsonMappingException is expected.");
    }

    private static void assertToStringMap(final Map<String, String> expected, final String inputJson) throws IOException {
        final ToStringMap toStringMap = MAPPER.readValue(inputJson, ToStringMap.class);
        assertEquals(expected, toStringMap);
    }

    private static void assertWithMap(final String inputJson) throws IOException {
        final ToStringMap toStringMap = MAPPER.readValue(inputJson, ToStringMap.class);
        final HashMap<String, String> map = MAPPER.readValue(inputJson, new TypeReference<HashMap<String, String>>() {});
        assertEquals(map, toStringMap);
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
