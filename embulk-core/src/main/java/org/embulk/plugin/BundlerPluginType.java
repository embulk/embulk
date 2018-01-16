package org.embulk.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class BundlerPluginType
        extends PluginType
{
    private BundlerPluginType(final String name, final String scope)
    {
        super("bundler", name);
        this.scope = scope;

        final StringBuilder fullNameBuilder = new StringBuilder();
        fullNameBuilder.append("bundler:");
        fullNameBuilder.append(scope);
        fullNameBuilder.append(":");
        fullNameBuilder.append(name);
        this.fullName = fullNameBuilder.toString();

        final HashMap<String, String> fullMapMutable = new HashMap<String, String>();
        fullMapMutable.put("source", "bundler");
        fullMapMutable.put("name", name);
        fullMapMutable.put("scope", scope);
        this.fullMap = Collections.unmodifiableMap(fullMapMutable);
    }

    public static BundlerPluginType create(final String name, final String scope)
    {
        if (name == null || scope == null) {
            throw new NullPointerException("\"name\", \"scope\" must be present.");
        }
        return new BundlerPluginType(name, scope);
    }

    @JsonValue
    public final Map<String, String> getJsonValue()
    {
        return this.fullMap;
    }

    @JsonProperty("scope")
    public final String getScope()
    {
        return this.scope;
    }

    public final String getFullName()
    {
        return this.fullName;
    }

    @Override
    public final int hashCode()
    {
        return Objects.hash(getSourceType(), getName(), this.scope);
    }

    @Override
    public boolean equals(Object objectOther)
    {
        if (!(objectOther instanceof BundlerPluginType)) {
            return false;
        }
        BundlerPluginType other = (BundlerPluginType) objectOther;
        return (this.getSourceType().equals(other.getSourceType()) &&
                this.getName().equals(other.getName()) &&
                this.getScope().equals(other.getScope()));
    }

    @JsonValue
    @Override
    public String toString()
    {
        return this.fullName;
    }

    private final String scope;
    private final String fullName;
    private final Map<String, String> fullMap;
}
