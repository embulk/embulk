package org.quickload.config;

import java.lang.reflect.Method;
import com.google.common.base.Optional;
import com.google.common.base.Function;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConfigSource
        extends TaskSource
{
    public ConfigSource(ModelManager modelManager, ObjectNode source)
    {
        super(modelManager, source, new Function<Method, Optional<String>>() {
            public Optional<String> apply(Method getterMethod)
            {
                return configJsonKey(getterMethod);
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
}
