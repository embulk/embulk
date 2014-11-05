package org.quickload.config;

import java.io.IOException;
import java.lang.reflect.Method;
import com.google.common.base.Optional;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ConfigSource
        extends DataSource<ConfigSource>
{
    private static final FieldMapper fieldMapper = new FieldMapper() {
        @Override
        public Optional<String> getJsonKey(Method getterMethod)
        {
            return configJsonKey(getterMethod);
        }

        @Override
        public Optional<String> getDefaultJsonString(Method getterMethod)
        {
            return configDefaultJsonValue(getterMethod);
        }
    };

    public ConfigSource()
    {
        super(fieldMapper);
    }

    /**
     * visible for DataSourceSerDe
     */
    ConfigSource(ObjectNode data)
    {
        super(data, fieldMapper);
    }

    public static ConfigSource fromJson(JsonParser parser) throws IOException
    {
        JsonNode json = new ObjectMapper().readTree(parser);
        if (!json.isObject()) {
            throw new JsonMappingException("Expected object to deserialize ConfigSource but got "+json);
        }
        return fromJson((ObjectNode) json);
    }

    public static ConfigSource fromJson(ObjectNode data)
    {
        return new ConfigSource(data.deepCopy());
    }

    private static Optional<String> configJsonKey(Method getterMethod)
    {
        Config a = getterMethod.getAnnotation(Config.class);
        if (a != null) {
            return Optional.of(a.value());
        } else {
            return Optional.absent();
        }
    }

    private static Optional<String> configDefaultJsonValue(Method getterMethod)
    {
        Config a = getterMethod.getAnnotation(Config.class);
        if (a != null && !a.defaultValue().isEmpty()) {
            return Optional.of(a.defaultValue());
        } else {
            return Optional.absent();
        }
    }
}
