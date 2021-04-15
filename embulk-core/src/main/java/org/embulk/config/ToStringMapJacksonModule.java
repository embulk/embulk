package org.embulk.config;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.embulk.spi.unit.ToStringMap;

@Deprecated
public final class ToStringMapJacksonModule extends SimpleModule {
    public ToStringMapJacksonModule() {
        this.addDeserializer(ToStringMap.class, new ToStringMapDeserializer());
    }

    private static class ToStringMapDeserializer extends JsonDeserializer<ToStringMap> {
        @Override
        public ToStringMap deserialize(
                final JsonParser jsonParser,
                final DeserializationContext context)
                throws JsonMappingException {
            final JsonNode jsonNode;
            try {
                jsonNode = OBJECT_MAPPER.readTree(jsonParser);
            } catch (final JsonParseException ex) {
                throw JsonMappingException.from(jsonParser, "Failed to parse JSON.", ex);
            } catch (final JsonProcessingException ex) {
                throw JsonMappingException.from(jsonParser, "Failed to process JSON in parsing.", ex);
            } catch (final IOException ex) {
                throw JsonMappingException.from(jsonParser, "Failed to read JSON in parsing.", ex);
            }

            if (jsonNode == null || !jsonNode.isObject()) {
                throw JsonMappingException.from(jsonParser, "ToStringMap expects a JSON object node.");
            }
            final ObjectNode node = (ObjectNode) jsonNode;

            final HashMap<String, String> built = new HashMap<String, String>();
            for (final Map.Entry<String, JsonNode> entry : (Iterable<Map.Entry<String, JsonNode>>) () -> node.fields()) {
                final JsonNode value = entry.getValue();
                if (value == null || value.isNull()) {
                    built.put(entry.getKey(), "null");
                } else {
                    built.put(entry.getKey(), ToStringJacksonModule.jsonNodeToString(value, jsonParser));
                }
            }
            return ToStringMap.of(Collections.unmodifiableMap(built));
        }

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    }
}
