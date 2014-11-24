package org.quickload.config;

import java.util.Map;
import java.io.IOException;
import java.lang.reflect.Type;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import org.quickload.config.ModelManager;

public class EnumTaskSerDe
{
    public static void configure(ObjectMapperModule mapper)
    {
        mapper.registerModule(new EnumTaskSerializerModule());
        mapper.registerModule(new EnumTaskDeserializerModule());
    }

    public static class EnumTaskSerializer
            extends JsonSerializer<EnumTask>
    {
        @Override
        public void serialize(EnumTask value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException
        {
            jgen.writeString(value.getName());
        }
    }

    public static class EnumTaskDeserializer <T extends EnumTask>
            extends FromStringDeserializer<T>
    {
        private final Map<String, T> map;

        public EnumTaskDeserializer(Class<T> enumClass)
        {
            super(enumClass);
            ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
            for (T e : enumClass.getEnumConstants()) {
                builder.put(e.getName(), e);
            }
            map = builder.build();
        }

        @Override
        protected T _deserialize(String value, DeserializationContext context)
                throws JsonMappingException
        {

            T e = map.get(value);
            if (e == null) {
                throw new JsonMappingException(
                        String.format("Unknown enum '%s'. It must be either of: %s",
                            value,
                            Joiner.on(", ").join(map.keySet())));
            }
            return e;
        }
    }

    public static class EnumTaskSerializerModule
            extends SimpleModule
    {
        public EnumTaskSerializerModule()
        {
            super();
            addSerializer(EnumTask.class, new EnumTaskSerializer());
        }
    }

    public static class EnumTaskDeserializerModule
            extends Module // can't use just SimpleModule, due to generic types
    {
        @Override
        public String getModuleName() { return "quickload.config.EnumTaskSerDe"; }

        @Override
        public Version version() { return Version.unknownVersion(); }

        @Override
        public void setupModule(SetupContext context)
        {
            context.addDeserializers(new Deserializers.Base() {
                @Override
                public JsonDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config,
                        BeanDescription beanDesc) throws JsonMappingException
                {
                    Class<?> raw = type.getRawClass();
                    if (EnumTask.class.isAssignableFrom(raw)) {
                        return newEnumTaskDeserializer((Class<? extends EnumTask>) raw);
                    }
                    return super.findBeanDeserializer(type, config, beanDesc);
                }
            });
        }

        protected JsonDeserializer<?> newEnumTaskDeserializer(Class<? extends EnumTask> raw)
        {
            return new EnumTaskDeserializer(raw);
        }
    }
}
