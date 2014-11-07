package org.quickload.config;

import java.lang.reflect.Method;
import com.google.common.base.Optional;

public abstract class FieldMapper
{
    public abstract Optional<String> getJsonKey(Method getterMethod);

    public Optional<String> getDefaultJsonString(Method getterMethod)
    {
        return Optional.absent();
    }
}
