package org.embulk.spi.type;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.IOException;
import org.junit.Test;

public class TestTypeSerDe {

    private static class HasType {
        private Type type;
        // TODO test TimestampType

        @JsonCreator
        public HasType(
                @JsonProperty("type") Type type) {
            this.type = type;
        }

        @JsonProperty("type")
        public Type getType() {
            return type;
        }
    }

    @Test
    public void testGetType() throws IOException {
        HasType type = new HasType(StringType.STRING);
        String json = MAPPER.writeValueAsString(type);
        HasType decoded = MAPPER.readValue(json, HasType.class);
        assertTrue(StringType.STRING == decoded.getType());
    }

    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new Jdk8Module());
        MAPPER.registerModule(new TypeJacksonModule());
    }
}
