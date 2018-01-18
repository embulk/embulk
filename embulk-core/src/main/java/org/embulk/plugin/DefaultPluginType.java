package org.embulk.plugin;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public final class DefaultPluginType extends PluginType {
    private DefaultPluginType(final String name) {
        super("default", name);
    }

    public static PluginType create(final String name) {
        if (name == null) {
            throw new NullPointerException("name must not be null");
        }
        return new DefaultPluginType(name);
    }

    @JsonValue
    public final String getJsonValue() {
        return this.getName();
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getSourceType(), getName());
    }

    @Override
    public final boolean equals(final Object objectOther) {
        if (!(objectOther instanceof DefaultPluginType)) {
            return false;
        }
        final DefaultPluginType other = (DefaultPluginType) objectOther;
        return (this.getSourceType().equals(other.getSourceType())
                && this.getName().equals(other.getName()));
    }

    @Override
    public final String toString() {
        return this.getName();
    }
}
