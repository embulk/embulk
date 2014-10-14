package org.quickload.spi;

import java.lang.reflect.Method;

class BasicPluginUtils
{
    static Class<?> getTaskType(Class<?> basicPlugin, String getTaskMethodName,
            Class<?>... parameterTypes)
    {
        try {
            Class<?> taskType = basicPlugin.getMethod(getTaskMethodName, parameterTypes).getReturnType();
            return basicPlugin.getMethod(getTaskMethodName, parameterTypes).getReturnType();
        } catch (NoSuchMethodException ex) {
            throw new AssertionError(
                    String.format("Basic plugin %s must implement %s method",
                        basicPlugin.getName(), getTaskMethodName), ex);
        }
    }
}
