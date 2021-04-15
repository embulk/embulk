package org.embulk.config;

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

final class TimestampFormatJacksonModule extends SimpleModule {
    @SuppressWarnings("deprecation")  // For use of org.embulk.spi.time.TimestampFormat
    public TimestampFormatJacksonModule() {
        this.addSerializer(org.embulk.spi.time.TimestampFormat.class, new TimestampFormatSerializer());
        this.addDeserializer(org.embulk.spi.time.TimestampFormat.class, new TimestampFormatDeserializer());
    }

    @SuppressWarnings("deprecation")  // For use of org.embulk.spi.time.TimestampFormat
    private static class TimestampFormatSerializer extends JsonSerializer<org.embulk.spi.time.TimestampFormat> {
        @Override
        public void serialize(
                final org.embulk.spi.time.TimestampFormat value,
                final JsonGenerator jsonGenerator,
                final SerializerProvider provider)
                throws IOException {
            jsonGenerator.writeString(value.getFormat());
        }
    }

    @SuppressWarnings("deprecation")  // For use of org.embulk.spi.time.TimestampFormat
    private static class TimestampFormatDeserializer extends JsonDeserializer<org.embulk.spi.time.TimestampFormat> {
        @Override
        public org.embulk.spi.time.TimestampFormat deserialize(
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

            return new org.embulk.spi.time.TimestampFormat(getString(node, jsonParser));
        }

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    }

    private static String getString(final JsonNode node, final JsonParser jsonParser) throws JsonMappingException {
        if (node.isTextual()) {
            return node.textValue();
        }
        throw JsonMappingException.from(jsonParser, "TimestampFormat must be a string.");
    }
}
