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

public abstract class PluginType
{
    public static final PluginType LOCAL = DefaultPluginType.create("local");

    /**
     * Constructs {@code PluginType}.
     *
     * The constructor is {@code protected} to be called from subclasses, e.g. {@code DefaultPluginType}.
     */
    protected PluginType(final String source, final String name)
    {
        this.source = source;
        this.name = name;
    }

    @JsonCreator
    private static PluginType create(final JsonNode typeJson)
    {
        if (typeJson.isTextual()) {
            return createFromString(((TextNode) typeJson).textValue());
        }
        else if (typeJson.isObject()) {
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
        }
        else {
            throw new IllegalArgumentException("\"type\" must be a string or a 1-depth mapping.");
        }
    }

    private static PluginType createFromString(String name)
    {
        if (name == null) {
            throw new NullPointerException("name must not be null");
        }
        return DefaultPluginType.create(name);
    }

    private static PluginType createFromStringMap(Map<String, String> stringMap)
    {
        final String source;
        if (stringMap.containsKey("source")) {
            source = stringMap.get("source");
        }
        else {
            source = DEFAULT;
        }

        switch (source) {
        case DEFAULT:
            {
                final String name = stringMap.get("name");
                return createFromString(name);
            }
        default:
            throw new IllegalArgumentException("\"source\" must be one of: [\"default\"]");
        }
    }

    @VisibleForTesting
    static PluginType createFromStringForTesting(final String name)
    {
        return createFromString(name);
    }

    @VisibleForTesting
    static PluginType createFromStringMapForTesting(final Map<String, String> object)
    {
        return createFromStringMap(object);
    }

    @JsonProperty("source")
    public final String getSource()
    {
        return source;
    }

    @JsonProperty("name")
    public final String getName()
    {
        return name;
    }

    private static final String DEFAULT = "default";

    private final String source;
    private final String name;
}
