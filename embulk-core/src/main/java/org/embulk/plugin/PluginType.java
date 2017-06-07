package org.embulk.plugin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import java.util.Map;

public abstract class PluginType
{
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
    public static PluginType createFromString(String name)
    {
        if (name == null) {
            throw new NullPointerException("name must not be null");
        }
        return DefaultPluginType.create(name);
    }

    @JsonCreator
    private static PluginType createFromStringMap(Map<String, String> object)
    {
        final String source;
        if (object.containsKey("source")) {
            source = object.get("source");
        }
        else {
            source = DEFAULT;
        }

        switch (source) {
        case DEFAULT:
            {
                final String name = object.get("name");
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
