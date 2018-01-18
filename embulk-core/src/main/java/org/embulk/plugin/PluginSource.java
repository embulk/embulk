package org.embulk.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface PluginSource {
    <T> T newPlugin(Class<T> iface, PluginType type) throws PluginSourceNotMatchException;

    public enum Type {
        DEFAULT("default"),  // DEFAULT includes InjectedPluginSource and JRubyPluginSource.
        MAVEN("maven"),
        ;

        private Type(final String sourceTypeName) {
            this.sourceTypeName = sourceTypeName;
        }

        public static Type of(final String sourceTypeName) {
            final Type found = MAP_FROM_STRING.get(sourceTypeName);
            if (found == null) {
                throw new IllegalArgumentException("\"" + sourceTypeName + "\" is not a plugin source.");
            }
            return found;
        }

        @Override
        public final String toString() {
            return this.sourceTypeName;
        }

        static {
            final HashMap<String, Type> mapToBuild = new HashMap<String, Type>();
            for (Type type : values()) {
                mapToBuild.put(type.sourceTypeName, type);
            }
            MAP_FROM_STRING = Collections.unmodifiableMap(mapToBuild);
        }

        private static final Map<String, Type> MAP_FROM_STRING;
        private final String sourceTypeName;
    }
}
