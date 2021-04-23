package org.embulk.deps.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;

final class ByteSizeJacksonModule extends SimpleModule {
    @SuppressWarnings("deprecation")  // For use of org.embulk.spi.unit.ByteSize
    public ByteSizeJacksonModule() {
        this.addSerializer(org.embulk.spi.unit.ByteSize.class, new ByteSizeSerializer());
        this.addDeserializer(org.embulk.spi.unit.ByteSize.class, new ByteSizeDeserializer());
    }

    @SuppressWarnings("deprecation")  // For use of org.embulk.spi.unit.ByteSize
    private static class ByteSizeSerializer extends JsonSerializer<org.embulk.spi.unit.ByteSize> {
        @Override
        public void serialize(
                final org.embulk.spi.unit.ByteSize value,
                final JsonGenerator jsonGenerator,
                final SerializerProvider provider)
                throws IOException {
            jsonGenerator.writeString(value.toString());
        }
    }

    @SuppressWarnings("deprecation")  // For use of org.embulk.spi.unit.ByteSize
    private static class ByteSizeDeserializer extends JsonDeserializer<org.embulk.spi.unit.ByteSize> {
        @Override
        public org.embulk.spi.unit.ByteSize deserialize(
                final JsonParser jsonParser,
                final DeserializationContext context)
                throws JsonMappingException {
            final JsonNode node;
            try {
                node = OBJECT_MAPPER.readTree(jsonParser);
            } catch (final JsonParseException ex) {
                throw JsonMappingException.from(jsonParser, "Failed to parse JSON.", ex);
            } catch (final JsonProcessingException ex) {
                throw JsonMappingException.from(jsonParser, "Failed to process JSON in parsing.", ex);
            } catch (final IOException ex) {
                throw JsonMappingException.from(jsonParser, "Failed to read JSON in parsing.", ex);
            }

            return getByteSize(node, jsonParser);
        }

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    }

    @SuppressWarnings("deprecation")  // For use of org.embulk.spi.unit.ByteSize
    private static org.embulk.spi.unit.ByteSize getByteSize(final JsonNode node, final JsonParser jsonParser) throws JsonMappingException {
        if (node.isTextual()) {
            return org.embulk.spi.unit.ByteSize.parseByteSize(node.asText());
        } else if (node.isIntegralNumber()) {
            return new org.embulk.spi.unit.ByteSize(node.asLong());
        }
        throw JsonMappingException.from(jsonParser, "ByteSize must be a string or an integer.");
    }
}
