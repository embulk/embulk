package org.quickload.config;

import java.io.IOException;
import java.lang.reflect.Type;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.core.type.TypeReference;

public class ConfigSource
{
    private final DynamicModeler dynamicModeler;
    private ModelRegistry modelRegistry;

    private final Map<String, String> configData;

    public ConfigSource(ModelRegistry modelRegistry, Map<String, String> configData)
    {
        this.modelRegistry = modelRegistry;
        this.configData = configData;
        this.dynamicModeler = new DynamicModeler(modelRegistry);
    }

    // TODO ConfigSource merge(Map<String, String> data)

    public <T extends DynamicModel<T>> T load(Class<T> iface)
    {
        return load(dynamicModeler.model(iface), iface);
    }

    public <T> T load(T obj)
    {
        return load(obj, (Class<T>) obj.getClass());
    }

    public <T> T load(T obj, Class<? extends T> klass)
    {
        for (Method method : klass.getMethods()) {
            try {
                Config annotation = method.getAnnotation(Config.class);
                if (annotation != null && method.getParameterTypes().length == 0) {
                    // @Config field
                    String methodName = method.getName();

                    String attrName;
                    if (methodName.startsWith("get")) {
                        attrName = methodName.substring(3);
                    } else {
                        throw new IllegalArgumentException(
                                String.format("Name of the method %s.%s with @Config annotation must start with 'get'",
                                    klass, methodName));
                    }

                    Method getter;
                    try {
                        getter = klass.getMethod(methodName);
                    } catch (NoSuchMethodException ex) {
                        throw new IllegalArgumentException(
                                String.format("The getter method @Config %s.%s must have 0 arguments",
                                    klass, methodName));
                    }

                    setConfigValue(obj, klass, getter, attrName, annotation);
                }

            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else if (cause instanceof Error) {
                    throw (Error) cause;
                } else {
                    throw new ConfigException(cause);
                }

            } catch (IllegalAccessException ex) {
                throw new ConfigException(ex);
            }
        }

        return obj;
    }

    private <T> void setConfigValue(T obj, Class<? extends T> klass,
            Method getter, String attrName, Config annotation) throws InvocationTargetException, IllegalAccessException
    {
        String configName = annotation.value();

        // TODO pass the json string directry to the setter method if its argument has @ConfigJson annotation

        // find setXxx(SerializableType object)
        try {
            Method setter = klass.getMethod("set" + attrName, Object.class);
            String value = configData.get(configName);
            if (value != null) {
                // use argument type of the setter method
                setter.invoke(obj, convertConfigValue(value, setter.getGenericParameterTypes()[0]));
            } else {
                // TODO get default value from Config.defaultValue
            }
            return;
        } catch (NoSuchMethodException ex) {
        }

        // find set(String attrName, SerializableType object)
        try {
            Method globalSetter = klass.getMethod("set", String.class, Object.class);
            String value = configData.get(configName);
            if (value != null) {
                // use return value type of the getter method
                globalSetter.invoke(obj, attrName, convertConfigValue(value, getter.getGenericReturnType()));
            } else {
                // TODO get default value from Config.defaultValue
            }
            return;
        } catch (NoSuchMethodException ex) {
        }

        throw new IllegalArgumentException(
                String.format("Configuration %s has @Config %s method but does not have set%s(Object) or set(\"%s\", Object) method",
                    klass, getter.getName(), attrName, attrName));
    }

    private Object convertConfigValue(String value, final Type type)
    {
        try {
            return modelRegistry.getObjectMapper().readValue(value, new TypeReference<Object>() {
                    public Type getType()
                    {
                        return type;
                    }
                });
        } catch (IOException ex) {
            // must not happen
            throw new ConfigException(ex);
        }
    }
}
