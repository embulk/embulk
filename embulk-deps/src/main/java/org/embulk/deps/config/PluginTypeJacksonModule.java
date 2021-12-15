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
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.embulk.plugin.DefaultPluginType;
import org.embulk.plugin.MavenPluginType;
import org.embulk.plugin.PluginSource;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.maven.MavenExcludeDependency;
import org.embulk.plugin.maven.MavenIncludeDependency;

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
                final Set<MavenExcludeDependency> excludeDependencies = getExcludeDependencies(typeObject, "type");
                final Set<MavenIncludeDependency> includeDependencies = getIncludeDependencies(typeObject, "type");
                return MavenPluginType.create(name, group, classifier, version, excludeDependencies, includeDependencies);
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

    private static Set<MavenExcludeDependency> getExcludeDependencies(final ObjectNode object, final String parent) {
        final LinkedHashSet<MavenExcludeDependency> excludeDependencies = new LinkedHashSet<>();

        final JsonNode excludeDependenciesJson = object.get("exclude_dependencies");
        if (excludeDependenciesJson != null) {
            if (!excludeDependenciesJson.isArray()) {
                throw new IllegalArgumentException("\"exclude_dependencies\" in \"" + parent + "\" must be an array.");
            }
            for (final JsonNode excludeDependencyJson : (ArrayNode) excludeDependenciesJson) {
                excludeDependencies.add(getExcludeDependency(excludeDependencyJson));
            }
        }

        final JsonNode overrideDependenciesJson = object.get("override_dependencies");
        if (overrideDependenciesJson != null) {
            if (!overrideDependenciesJson.isArray()) {
                throw new IllegalArgumentException("\"override_dependencies\" in \"" + parent + "\" must be an array.");
            }
            for (final JsonNode overrideDependencyJson : (ArrayNode) overrideDependenciesJson) {
                excludeDependencies.add(getOverrideDependency(overrideDependencyJson).toMavenExcludeDependency());
            }
        }

        return Collections.unmodifiableSet(excludeDependencies);
    }

    private static Set<MavenIncludeDependency> getIncludeDependencies(final ObjectNode object, final String parent) {
        final LinkedHashSet<MavenIncludeDependency> includeDependencies = new LinkedHashSet<>();

        final JsonNode overrideDependenciesJson = object.get("override_dependencies");
        if (overrideDependenciesJson != null) {
            if (!overrideDependenciesJson.isArray()) {
                throw new IllegalArgumentException("\"override_dependencies\" in \"" + parent + "\" must be an array.");
            }
            for (final JsonNode overrideDependencyJson : (ArrayNode) overrideDependenciesJson) {
                includeDependencies.add(getOverrideDependency(overrideDependencyJson));
            }
        }

        return Collections.unmodifiableSet(includeDependencies);
    }

    private static MavenExcludeDependency getExcludeDependency(final JsonNode json) {
        if (json.isArray()) {
            throw new IllegalArgumentException("Elements in \"exclude_dependencies\" must not be an array.");
        }
        if (json.isObject()) {
            final ObjectNode excludeDependencyObject = (ObjectNode) json;
            final String artifactId = getTextual(excludeDependencyObject, "artifactId", "exclude_dependencies");
            final String groupId = getTextual(excludeDependencyObject, "groupId", "exclude_dependencies");
            final String classifier = getTextual(excludeDependencyObject, "classifier", "exclude_dependencies");
            return MavenExcludeDependency.of(groupId, artifactId, classifier);
        }
        return MavenExcludeDependency.fromString(json.asText());
    }

    private static MavenIncludeDependency getOverrideDependency(final JsonNode json) {
        if (json.isArray()) {
            throw new IllegalArgumentException("Elements in \"override_dependencies\" must not be an array.");
        }
        if (json.isObject()) {
            final ObjectNode overrideDependencyObject = (ObjectNode) json;
            final String artifactId = getTextual(overrideDependencyObject, "artifactId", "override_dependencies");
            final String groupId = getTextual(overrideDependencyObject, "groupId", "override_dependencies");
            final String version = getTextual(overrideDependencyObject, "version", "override_dependencies");
            final String classifier = getTextual(overrideDependencyObject, "classifier", "override_dependencies");
            return MavenIncludeDependency.of(groupId, artifactId, version, classifier);
        }
        return MavenIncludeDependency.fromString(json.asText());
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
