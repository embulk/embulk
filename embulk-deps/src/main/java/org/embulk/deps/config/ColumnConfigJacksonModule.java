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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.spi.ColumnConfig;
import org.embulk.spi.type.Type;

public final class ColumnConfigJacksonModule extends SimpleModule {
    public ColumnConfigJacksonModule(final ModelManagerDelegateImpl model) {
        this.addSerializer(ColumnConfig.class, new ColumnConfigSerializer(model));
        this.addDeserializer(ColumnConfig.class, new ColumnConfigDeserializer(model));
    }

    private static class ColumnConfigSerializer extends JsonSerializer<ColumnConfig> {
        ColumnConfigSerializer(final ModelManagerDelegateImpl model) {
            this.model = model;
        }

        @Override
        public void serialize(
                final ColumnConfig value,
                final JsonGenerator jsonGenerator,
                final SerializerProvider provider)
                throws IOException {
            final ConfigSource option = value.getOption();
            final ObjectNode object = model.writeObjectAsObjectNode(option);
            object.put("name", value.getName());
            object.put("type", value.getType().getName());
            jsonGenerator.writeTree(object);
        }

        private final ModelManagerDelegateImpl model;
    }

    private static class ColumnConfigDeserializer extends JsonDeserializer<ColumnConfig> {
        ColumnConfigDeserializer(final ModelManagerDelegateImpl model) {
            this.model = model;
        }

        @Override
        public ColumnConfig deserialize(
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

            if (!node.isObject()) {
                throw new JsonMappingException("Expected object to deserialize ColumnConfig", jsonParser.getCurrentLocation());
            }

            try {
                final ConfigSource config = (ConfigSource) new DataSourceImpl(model, (ObjectNode) node);

                final String name = config.get(String.class, "name");
                final Type type = config.get(Type.class, "type");
                final ConfigSource option = config.deepCopy();
                option.remove("name");
                option.remove("type");

                return new ColumnConfig(name, type, option);
            } catch (final ConfigException ex) {
                throw JsonMappingException.from(jsonParser, "Invalid object to deserialize ColumnConfig", ex);
            }
        }

        private final ModelManagerDelegateImpl model;
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
