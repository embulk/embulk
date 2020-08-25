package org.embulk.spi.type;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;

public final class TypeJacksonModule extends SimpleModule {
    public TypeJacksonModule() {
        this.addSerializer(Type.class, new TypeSerializer());
        this.addDeserializer(Type.class, new TypeDeserializer());
    }

    private static class TypeSerializer extends JsonSerializer<Type> {
        @Override
        public void serialize(
                final Type value,
                final JsonGenerator jsonGenerator,
                final SerializerProvider provider)
                throws IOException {
            jsonGenerator.writeString(value.getName());
        }
    }
}
