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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import org.embulk.config.ConfigSource;
import org.embulk.spi.ColumnConfig;
import org.embulk.spi.SchemaConfig;

public final class SchemaConfigJacksonModule extends SimpleModule {
    public SchemaConfigJacksonModule(final ModelManagerDelegateImpl model) {
        this.addSerializer(SchemaConfig.class, new SchemaConfigSerializer(model));
        this.addDeserializer(SchemaConfig.class, new SchemaConfigDeserializer(model));
    }

    private static class SchemaConfigSerializer extends JsonSerializer<SchemaConfig> {
        SchemaConfigSerializer(final ModelManagerDelegateImpl model) {
            this.model = model;
        }

        @Override
        public void serialize(
                final SchemaConfig value,
                final JsonGenerator jsonGenerator,
                final SerializerProvider provider)
                throws IOException {
            final ArrayNode array = OBJECT_MAPPER.createArrayNode();

            for (final ColumnConfig columnConfig : value.getColumns()) {
                final ConfigSource option = columnConfig.getOption();
                final ObjectNode object = this.model.writeObjectAsObjectNode(option);
                object.put("name", columnConfig.getName());
                object.put("type", columnConfig.getType().getName());
                array.add(object);
            }
            jsonGenerator.writeTree(array);
        }

        private final ModelManagerDelegateImpl model;
    }

    private static class SchemaConfigDeserializer extends JsonDeserializer<SchemaConfig> {
        SchemaConfigDeserializer(final ModelManagerDelegateImpl model) {
            this.model = model;
        }

        @Override
        public SchemaConfig deserialize(
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

            if (!node.isArray()) {
                throw new JsonMappingException("Expected array to deserialize SchemaConfig", jsonParser.getCurrentLocation());
            }
            final ArrayList<ColumnConfig> columnConfigs = new ArrayList<>();
            for (final JsonNode columnConfigNode : (ArrayNode) node) {
                columnConfigs.add(model.readObject(ColumnConfig.class, columnConfigNode.traverse()));
            }
            return new SchemaConfig(Collections.unmodifiableList(columnConfigs));
        }

        private final ModelManagerDelegateImpl model;
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
