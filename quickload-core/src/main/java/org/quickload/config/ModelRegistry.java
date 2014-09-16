package org.quickload.config;

import java.util.Set;
import java.util.HashSet;
import com.google.common.base.Function;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ModelRegistry
{
    private final Set<Class<?>> registered;
    private final SimpleModule module;
    private final ObjectMapper objectMapper;

    public ModelRegistry()
    {
        this.registered = new HashSet<Class<?>>();
        this.module = new SimpleModule();
        this.objectMapper = new ObjectMapper().findAndRegisterModules().registerModule(module);
    }

    public synchronized void add(Class<?> klass, Function<ObjectMapper, Void> generator)
    {
        if (registered.contains(klass)) {
            return;
        }
        generator.apply(objectMapper);
        registered.add(klass);
    }

    public ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }
}
