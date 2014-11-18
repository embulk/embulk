package org.quickload.config;

import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import javax.validation.Validation;
import org.apache.bval.jsr303.ApacheValidationProvider;
import com.google.inject.Inject;
import com.google.common.base.Throwables;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;

public class ModelManager
{
    private final ObjectMapper objectMapper;
    private final ObjectMapper objectMapper2;
    private final TaskValidator taskValidator;

    @Inject
    public ModelManager()
    {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.objectMapper2 = new ObjectMapper().findAndRegisterModules();
        this.taskValidator = new TaskValidator(
                Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory().getValidator());

        SimpleModule modelModule = new SimpleModule();
        //modelModule.addDeserializer(Task.class, new TaskDeserializer());
        modelModule.addSerializer(Task.class, new TaskSerializer());
        addObjectMapperModule(modelModule);
        addObjectMapperModule(new TaskDeserializerModule());
    }

    // TODO inject by Set<Module> because this is not thread-safe?
    public void addObjectMapperModule(Module module)
    {
        objectMapper.registerModule(module);
        objectMapper2.registerModule(module);
    }

    public <T> T readObject(DataSource<?> json, Class<T> valueType)
    {
        return readObject(json.getSource().traverse(), valueType);
    }

    public <T> T readObject(JsonParser json, Class<T> valueType)
    {
        try {
            ContextAttributes attr = ContextAttributes.getEmpty().withSharedAttribute("org.quickload.config.FieldMapper", null);
            return objectMapper.reader(attr).readValue(json, valueType);
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public <T extends Task> T readTaskConfig(DataSource<?> json, Class<T> taskType)
    {
        return readTaskConfig(json.getSource().traverse(), taskType);
    }

    public <T extends Task> T readTaskConfig(JsonParser json, Class<T> taskType)
    {
        T t;
        try {
            ContextAttributes attr = ContextAttributes.getEmpty().withSharedAttribute("org.quickload.config.FieldMapper", configFieldMapper);
            t = objectMapper2.reader(attr).readValue(json, taskType);
        } catch (Exception ex) {
            throw new ConfigException(ex);
        }
        t.validate();
        return t;
    }

    private static final FieldMapper configFieldMapper = new FieldMapper() {
        @Override
        public Optional<String> getJsonKey(Method getterMethod)
        {
            Config a = getterMethod.getAnnotation(Config.class);
            if (a != null) {
                return Optional.of(a.value());
            } else {
                return Optional.absent();
            }
        }

        @Override
        public Optional<String> getDefaultJsonString(Method getterMethod)
        {
            ConfigDefault a = getterMethod.getAnnotation(ConfigDefault.class);
            if (a != null && !a.value().isEmpty()) {
                return Optional.of(a.value());
            } else {
                return Optional.absent();
            }
        }
    };

    public String writeAsJsonString(Object object)
    {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public TaskSource writeAsTaskSource(Object object)
    {
        String json = writeAsJsonString(object);
        try {
            return new TaskSource((ObjectNode) objectMapper.readTree(json));
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
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

            String jsonKey;
            Optional<String> defaultJsonString;
            if (fieldMapper == null) {
                jsonKey = fieldName;
                defaultJsonString = Optional.absent();
            } else {
                Optional<String> key = fieldMapper.getJsonKey(method);
                if (!key.isPresent()) {
                    // skip this field
                    continue;
                }
                jsonKey = key.get();
                defaultJsonString = fieldMapper.getDefaultJsonString(method);
            }
            builder.put(jsonKey, new FieldEntry(fieldName, fieldType, defaultJsonString));
        }
        return builder.build();
    }

    class TaskDeserializers
            extends Deserializers.Base
    {
        @Override
        public JsonDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config,
                BeanDescription beanDesc) throws JsonMappingException
        {
            Class<?> raw = type.getRawClass();
            if (Task.class.isAssignableFrom(raw)) {
                return new TaskDeserializer(raw);
            }
            return super.findBeanDeserializer(type, config, beanDesc);
        }
    }

    class TaskDeserializerModule
        extends Module // can't use just SimpleModule, due to generic types
    {
        @Override
        public String getModuleName()
        {
            return "quickload.TaskDeserializerModule";
        }

        @Override
        public Version version()
        {
            return Version.unknownVersion();
        }

        @Override
        public void setupModule(SetupContext context)
        {
            context.addDeserializers(new TaskDeserializers());
        }
    }

    class TaskDeserializer <T>
            extends JsonDeserializer<T>
            implements ContextualDeserializer
    {
        private final Class<?> iface;
        private final Map<String, FieldEntry> mappings;

        public TaskDeserializer(Class<T> iface)
        {
            this(iface, null);
        }

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
            //current = jp.nextToken();
            current = jp.getCurrentToken();
            if (current == JsonToken.START_OBJECT) {
                current = jp.nextToken();
            }
            //if (current != JsonToken.START_OBJECT) {
            //    throw new JsonMappingException("Expected object to deserialize "+iface, jp.getCurrentLocation());
            //}
            //while (jp.nextToken() != JsonToken.END_OBJECT) {
            for (; current != JsonToken.END_OBJECT; current = jp.nextToken()) {
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
                    Object value = objectMapper.readValue(field.getDefaultJsonString().get(), new GenericTypeReference(field.getType()));
                    objects.put(field.getName(), value);
                } else {
                    // required field
                    throw new JsonMappingException("Field '"+unused.getKey()+"' is required but not set", jp.getCurrentLocation());
                }
            }

            return (T) Proxy.newProxyInstance(
                    iface.getClassLoader(), new Class<?>[] { iface },
                    new TaskInvocationHandler(iface, taskValidator, objects));
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) throws JsonMappingException
        {
            FieldMapper mapper = (FieldMapper) context.getAttribute("org.quickload.config.FieldMapper");
            return new TaskDeserializer(iface, mapper);
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
