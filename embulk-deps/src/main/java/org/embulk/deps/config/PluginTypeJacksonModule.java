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
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
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
                return createFromObjectNode((ObjectNode) typeJson);
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

    private static PluginType createFromObjectNode(final ObjectNode typeObject) {
        final PluginSource.Type sourceType;
        if (typeObject.has("source")) {
            sourceType = PluginSource.Type.of(getTextual(typeObject, "source", "type"));
        } else {
            sourceType = PluginSource.Type.DEFAULT;
        }

        switch (sourceType) {
            case DEFAULT: {
                final String name = getTextual(typeObject, "name", "type");
                return createFromString(name);
            }
            case MAVEN: {
                final String name = getTextual(typeObject, "name", "type");
                final String group = getTextual(typeObject, "group", "type");
                final String classifier = getTextual(typeObject, "classifier", "type");
                final String version = getTextual(typeObject, "version", "type");
                return MavenPluginType.create(name, group, classifier, version);
            }
            default:
                throw new IllegalArgumentException("\"source\" must be one of: [\"default\", \"maven\"]");
        }
    }

    static PluginType createFromStringForTesting(final String name) {
        return createFromString(name);
    }

    static PluginType createFromObjectNodeForTesting(final ObjectNode typeObject) {
        return createFromObjectNode(typeObject);
    }

    private static String getTextual(final ObjectNode object, final String fieldName, final String parent) {
        final JsonNode json = object.get(fieldName);
        if (json == null) {
            return null;
        }
        if (!json.isTextual()) {
            throw new IllegalArgumentException("\"" + fieldName + "\" in \"" + parent + "\" must be a textual value.");
        }
        return json.textValue();
    }
}
