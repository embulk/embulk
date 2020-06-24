package org.embulk.spi.time;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import java.io.IOException;

public class TimestampSerDe {
    public static void configure(ObjectMapperModule mapper) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Timestamp.class, new TimestampSerializer());
        module.addDeserializer(Timestamp.class, new TimestampDeserializer());
        mapper.registerModule(module);
    }

    public static class TimestampSerializer extends JsonSerializer<Timestamp> {
        @Override
        public void serialize(Timestamp value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.toString());
        }
    }

    public static class TimestampDeserializer extends FromStringDeserializer<Timestamp> {
        public TimestampDeserializer() {
            super(Timestamp.class);
        }

        @Override
        protected Timestamp _deserialize(String value, DeserializationContext context)
                throws JsonMappingException {
            if (value == null) {
                throw new JsonMappingException("TimestampDeserializer#_deserialize received null unexpectedly.");
            }
            try {
                return Timestamp.ofString(value);
            } catch (final NumberFormatException ex) {
                throw new JsonMappingException("Invalid format as a Timestamp value: '" + value + "'", ex);
            } catch (final IllegalStateException ex) {
                throw new JsonMappingException("Unexpected failure in parsing: '" + value + "'", ex);
            }
        }
    }
}
