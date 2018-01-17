package org.embulk.plugin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class PluginType {
    public static final PluginType LOCAL = DefaultPluginType.create("local");

    /**
     * Constructs {@code PluginType}.
     *
     * The constructor is {@code protected} to be called from subclasses, e.g. {@code DefaultPluginType}.
     */
    protected PluginType(final String source, final String name) {
        this.sourceType = PluginSource.Type.of(source);
        this.name = name;
    }

    @JsonCreator
    private static PluginType create(final JsonNode typeJson) {
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

    @VisibleForTesting
    static PluginType createFromStringForTesting(final String name) {
        return createFromString(name);
    }

    @VisibleForTesting
    static PluginType createFromStringMapForTesting(final Map<String, String> stringMap) {
        return createFromStringMap(stringMap);
    }

    public final PluginSource.Type getSourceType() {
        return sourceType;
    }

    @JsonProperty("source")
    public final String getSourceName() {
        return sourceType.toString();
    }

    @JsonProperty("name")
    public final String getName() {
        return name;
    }

    private final PluginSource.Type sourceType;
    private final String name;
}
