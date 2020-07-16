package org.embulk.spi.unit;

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
import com.fasterxml.jackson.databind.node.NullNode;
import java.io.IOException;

@Deprecated
public final class ToStringJacksonModule extends SimpleModule {
    @SuppressWarnings("deprecation")
    public ToStringJacksonModule() {
        this.addSerializer(ToString.class, new ToStringSerializer());
        this.addDeserializer(ToString.class, new ToStringDeserializer());
    }

    @SuppressWarnings("deprecation")
    private static class ToStringSerializer extends JsonSerializer<ToString> {
        @Override
        public void serialize(
                final ToString value,
                final JsonGenerator jsonGenerator,
                final SerializerProvider provider)
                throws IOException {
            jsonGenerator.writeString(value.toString());
        }
    }

    @SuppressWarnings("deprecation")
    private static class ToStringDeserializer extends JsonDeserializer<ToString> {
        @Override
        public ToString deserialize(
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

            return new ToString(jsonNodeToString(node != null ? node : NullNode.getInstance(), jsonParser));
        }

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    }

    static String jsonNodeToString(final JsonNode node, final JsonParser jsonParser) throws JsonMappingException {
        if (node.isTextual()) {
            return node.textValue();
        } else if (node.isValueNode()) {
            return node.toString();
        }
        throw JsonMappingException.from(jsonParser, String.format("Arrays and objects are invalid: '%s'", node));
    }
}
