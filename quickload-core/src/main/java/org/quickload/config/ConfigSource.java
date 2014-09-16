package org.quickload.config;

import java.io.IOException;
import java.lang.reflect.Method;

import com.google.common.base.Supplier;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ConfigSource
{
    private ObjectMapper objectMapper;
    /* TODO
    private final DynamicModeler dynamicModeler;
    private final Map<String, String> source;

    public ConfigSource(Map<String, String> source)
    {
        this.source = source;
    }

    public <T> T load(Class<T> klass)
    {
        T obj;
        if (klass.isInterface()) {
            obj = dynamicModeler.model(klass);
        } else {
        }
    }



    private static class ModelClassRegistry
    {
        private final Set<Class<?>> generatedClasses;
        private final SimpleModule generatedModule;
        private final ObjectMapper objectMapper;

        public ModelClassRegistry()
        {
            this.registered = new HashSet<Class<?>>();
            this.module = new SimpleModule();
            this.objectMapper = new ObjectMapper().findAndRegisterModules().withModule(module);
        }

        public synchronized add(Class<T> klass, Function<ObjectMapper, Void> generator)
        {
            if (generatedClasses.contains(klass)) {
                return false;
            }
            generator.apply(objectMapper);
            generatedClasses.add(klass);
        }

        public ObjectMapper getObjectMapper()
        {
            return objectMapper;
        }
    }

    private final String json;  // TODO JsonNode
    private final ModelClassRegistry registry;

    public ConfigSource(String json, ModelClassRegistry registry)
    {
        this.json = json;
        this.registry = registry;
    }

    public <T> T load(final Class<T> klass)
    {
        if (!klass.isInterface()) {
            return registry.getObjectMapper().readObject(json, klass);
        } else {
            registry.add(klass, new Function<ObjectMapper, Void>() {
                public Void apply(ObjectMapper dest)
                {
                    ConfigObjectMapperBuilder<T> builder = new ConfigObjectMapperBuilder<T>(klass);
                }
            });
        }
    }

    private static class ConfigObjectMapperBuilder <T>
    {
        public ConfigObjectMapperBuilder(Class<T> iface)
        {
            for (Method method : iface.getMethods()) {
                Config annotation = method.getAnnotation(Config.class);
                if (annotation != null && method.getParameterTypes().length == 0) {
                    addField(annotation.value(), method.getName());
                }
            }
        }

        private void addField(String configFieldName, String classAttrName)
        {
        }

        public void registerTo(ObjectMapper objectMapper)
        {
        }
    }

    private static class InterfaceDeserializer <T>
            extends JsonDeserializer<T>
    {
        private final ObjectMapper nestedObjectMapper;
        private final Map<String, Class<?>> fields;

        public InterfaceDeserializer(ObjectMapper nestedObjectMapper,
                Map<String, Class<?>> fields)
        {
            this.nestedObjectMapper = nestedObjectMapper;
            this.fields = fields;
        }

        @Override
        public T deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException
        {
            if (jp.nextToken() != JsonToken.START_OBJECT) {
                throw new RuntimeJsonMappingException("Expected object to deserialize config object");
            }

            HashMap<String, Object> map = new HashMap<String, Object>();

            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jp.getCurrentName();
                jp.nextToken();
                Class<?> fieldClass = fields.get(fieldName);
                if (fieldClass != null) {
                    Object value = nestedObjectMapper.read(jp, fieldClass, ctxt.getConfig());
                    map.put(fieldName, value);
                } else {
                    jp.skipChildren();
                }
            }
        }
    }
    */
}
