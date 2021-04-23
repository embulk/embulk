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
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.embulk.plugin.DefaultPluginType;
import org.embulk.plugin.MavenPluginType;
import org.embulk.plugin.PluginSource;
import org.embulk.plugin.PluginType;

final class PluginTypeJacksonModule extends SimpleModule {
    public PluginTypeJacksonModule() {
        this.addSerializer(DefaultPluginType.class, new DefaultPluginTypeSerializer());
        this.addSerializer(MavenPluginType.class, new MavenPluginTypeSerializer());
        this.addDeserializer(PluginType.class, new PluginTypeDeserializer());
    }

    private static class DefaultPluginTypeSerializer extends JsonSerializer<DefaultPluginType> {
        @Override
        public void serialize(
                final DefaultPluginType value, final JsonGenerator jsonGenerator, final SerializerProvider provider)
                throws IOException {
            jsonGenerator.writeString(value.getName());
        }
    }

    private static class MavenPluginTypeSerializer extends JsonSerializer<MavenPluginType> {
        @Override
        public void serialize(
                final MavenPluginType value, final JsonGenerator jsonGenerator, final SerializerProvider provider)
                throws IOException {
            final ObjectNode object = OBJECT_MAPPER.createObjectNode();
            object.put("source", value.getSourceName());
            object.put("name", value.getName());
            if (value.getGroup() != null) {
                object.put("group", value.getGroup());
            }
            if (value.getClassifier() != null) {
                object.put("classifier", value.getClassifier());
            }
            if (value.getVersion() != null) {
                object.put("version", value.getVersion());
            }
            jsonGenerator.writeTree(object);
        }

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    }

    private static class PluginTypeDeserializer extends JsonDeserializer<PluginType> {
        @Override
        public PluginType deserialize(
                final JsonParser jsonParser,
                final DeserializationContext context)
                throws JsonMappingException {
            final JsonNode typeJson;
            try {
                typeJson = OBJECT_MAPPER.readTree(jsonParser);
            } catch (final JsonParseException ex) {
                throw JsonMappingException.from(jsonParser, "Failed to parse JSON.", ex);
            } catch (final JsonProcessingException ex) {
                throw JsonMappingException.from(jsonParser, "Failed to process JSON in parsing.", ex);
            } catch (final IOException ex) {
                throw JsonMappingException.from(jsonParser, "Failed to read JSON in parsing.", ex);
            }

            if (typeJson.isTextual()) {
                return createFromString(((TextNode) typeJson).textValue());
            } else if (typeJson.isObject()) {
                final HashMap<String, String> stringMap = new HashMap<String, String>();
                final ObjectNode typeObject = (ObjectNode) typeJson;
                final Iterator<Map.Entry<String, JsonNode>> fieldIterator = typeObject.fields();
                while (fieldIterator.hasNext()) {
                    final Map.Entry<String, JsonNode> field = fieldIterator.next();
                    final JsonNode fieldValue = field.getValue();
                    if (fieldValue instanceof ContainerNode) {
                        throw new IllegalArgumentException("\"type\" must be a string or a 1-depth mapping.");
                    }
                    stringMap.put(field.getKey(), fieldValue.textValue());
                }
                return createFromStringMap(stringMap);
            } else {
                throw new IllegalArgumentException("\"type\" must be a string or a 1-depth mapping.");
            }
        }

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    }

    private static PluginType createFromString(String name) {
        if (name == null) {
            throw new NullPointerException("name must not be null");
        }
        return DefaultPluginType.create(name);
    }

    private static PluginType createFromStringMap(Map<String, String> stringMap) {
        final PluginSource.Type sourceType;
        if (stringMap.containsKey("source")) {
            sourceType = PluginSource.Type.of(stringMap.get("source"));
        } else {
            sourceType = PluginSource.Type.DEFAULT;
        }

        switch (sourceType) {
            case DEFAULT: {
                final String name = stringMap.get("name");
                return createFromString(name);
            }
            case MAVEN: {
                final String name = stringMap.get("name");
                final String group = stringMap.get("group");
                final String classifier = stringMap.get("classifier");
                final String version = stringMap.get("version");
                return MavenPluginType.create(name, group, classifier, version);
            }
            default:
                throw new IllegalArgumentException("\"source\" must be one of: [\"default\", \"maven\"]");
        }
    }

    static PluginType createFromStringForTesting(final String name) {
        return createFromString(name);
    }

    static PluginType createFromStringMapForTesting(final Map<String, String> stringMap) {
        return createFromStringMap(stringMap);
    }
}
