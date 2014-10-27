package org.quickload.config;

import java.lang.reflect.Method;
import com.google.common.base.Optional;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConfigSource
        extends TaskSource
{
    public ConfigSource(ModelManager modelManager, ObjectNode source)
    {
        super(modelManager, source, new FieldMapper()
            {
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
            });
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
