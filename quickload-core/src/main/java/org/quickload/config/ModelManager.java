package org.quickload.config;

import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import com.google.inject.Inject;
import javax.validation.Validation;
import org.apache.bval.jsr303.ApacheValidationProvider;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ModelManager
{
    private final ObjectMapper objectMapper;
    private final TaskValidator taskValidator;

    @Inject
    public ModelManager()
    {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.taskValidator = new TaskValidator(
                Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory().getValidator());

        SimpleModule modelModule = new SimpleModule();
        modelModule.addSerializer(Task.class, new TaskSerializer());
        addObjectMapperModule(modelModule);
    }

    // TODO inject by Set<Module> because this is not thread-safe?
    public void addObjectMapperModule(Module module)
    {
        objectMapper.registerModule(module);
    }

    public <T extends Task> T readTask(JsonNode json, Class<T> iface)
    {
        return readTask(json.traverse(), iface);
    }

    public <T extends Task> T readTask(JsonParser json, Class<T> iface)
    {
        return readTask(json, iface, new FieldMapper());
    }

    public <T extends Task> T readTask(JsonNode json, Class<T> iface,
            FieldMapper fieldMapper)
    {
        return readTask(json.traverse(), iface, fieldMapper);
    }

    public <T extends Task> T readTask(JsonParser json, Class<T> iface,
            FieldMapper fieldMapper)
    {
        try {
            return (T) new TaskDeserializer(iface, fieldMapper).deserialize(
                    json, objectMapper.getDeserializationContext());
        } catch (IOException ex) {
            // TODO exception class
            throw new RuntimeException(ex);
        }
    }

    public <T> T readJsonObject(JsonNode json, Class<T> klass)
    {
        return readJsonObject(json.traverse(), klass);
    }

    public <T> T readJsonObject(JsonParser json, Class<T> klass)
    {
        try {
            return objectMapper.readValue(json, klass);
        } catch (IOException ex) {
            // TODO exception class
            throw new RuntimeException(ex);
        }
    }

    public <T> String writeJson(T object)
    {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException ex) {
            // TODO exception class
            throw new RuntimeException(ex);
        }
    }

    public <T> ObjectNode writeJsonObjectNode(T object)
    {
        String json = writeJson(object);
        try {
            return (ObjectNode) objectMapper.readTree(json);
        } catch (IOException ex) {
            // TODO exception class
            throw new RuntimeException(ex);
        }
    }

    private static class FieldEntry
    {
        private final String name;
        private final Type type;
        private final Optional<String> defaultJsonString;

        public FieldEntry(String name, Type type, Optional<String> defaultJsonString)
        {
            this.name = name;
            this.type = type;
            this.defaultJsonString = defaultJsonString;
        }

        public String getName()
        {
            return name;
        }

        public Type getType()
        {
            return type;
        }

        public Optional<String> getDefaultJsonString()
        {
            return defaultJsonString;
        }
    }

    /**
     * jsonKey = (name, type)
     */
    static Map<String, FieldEntry> getterMappings(Class<?> iface, FieldMapper fieldMapper)
    {
        ImmutableMap.Builder<String, FieldEntry> builder = ImmutableMap.builder();
        for (Map.Entry<String, Method> getter : TaskInvocationHandler.fieldGetters(iface).entrySet()) {
            Method method = getter.getValue();
            String fieldName = getter.getKey();
            Type fieldType = method.getGenericReturnType();
            String jsonKey = fieldMapper.getJsonKey(method).or(fieldName);
            Optional<String> defaultJsonString = fieldMapper.getDefaultJsonString(method);
            builder.put(jsonKey, new FieldEntry(fieldName, fieldType, defaultJsonString));
        }
        return builder.build();
    }

    class TaskDeserializer <T>
            extends JsonDeserializer<T>
    {
        private final Class<T> iface;
        private final Map<String, FieldEntry> mappings;

        public TaskDeserializer(Class<T> iface, FieldMapper fieldMapper)
        {
            this.iface = iface;
            this.mappings = getterMappings(iface, fieldMapper);
        }

        @Override
        public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            Map<String, Object> objects = new ConcurrentHashMap<String, Object>();
            HashMap<String, FieldEntry> unusedMappings = new HashMap<>(mappings);

            JsonToken current;
            current = jp.nextToken();
            if (current != JsonToken.START_OBJECT) {
                throw new JsonMappingException("Expected object to deserialize "+iface, jp.getCurrentLocation());
            }
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String key = jp.getCurrentName();
                current = jp.nextToken();
                FieldEntry field = mappings.get(key);
                if (field == null) {
                    jp.skipChildren();
                } else {
                    Object value = objectMapper.readValue(jp, new GenericTypeReference(field.getType()));
                    objects.put(field.getName(), value);
                    unusedMappings.remove(key);
                }
            }

            // set default values
            for (Map.Entry<String, FieldEntry> unused : unusedMappings.entrySet()) {
                FieldEntry field = unused.getValue();
                if (field.getDefaultJsonString().isPresent()) {
                    Object value = objectMapper.readValue(jp, new GenericTypeReference(field.getType()));
                    objects.put(field.getName(), value);
                }
            }

            return (T) Proxy.newProxyInstance(
                    iface.getClassLoader(), new Class<?>[] { iface },
                    new TaskInvocationHandler(iface, taskValidator, objects));
        }
    }

    class TaskSerializer
            extends JsonSerializer<Task>
    {
        @Override
        public void serialize(Task value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException
        {
            if (value instanceof Proxy) {
                Object handler = Proxy.getInvocationHandler(value);
                if (handler instanceof TaskInvocationHandler) {
                    TaskInvocationHandler h = (TaskInvocationHandler) handler;
                    Map<String, Object> objects = h.getObjects();
                    jgen.writeStartObject();
                    for (Map.Entry<String, Object> pair : objects.entrySet()) {
                        jgen.writeFieldName(pair.getKey());
                        objectMapper.writeValue(jgen, pair.getValue());
                    }
                    jgen.writeEndObject();
                    return;
                }
            }
            // TODO exception class & message
            throw new UnsupportedOperationException("Serializing Task is not supported");
        }
    }
}
