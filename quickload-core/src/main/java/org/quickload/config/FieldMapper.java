package org.quickload.config;

import java.lang.reflect.Method;
import com.google.common.base.Optional;

public class FieldMapper
{
    public Optional<String> getJsonKey(Method getterMethod)
    {
        return Optional.absent();
    }

    public Optional<String> getDefaultJsonString(Method getterMethod)
    {
        return Optional.absent();
    }
}
