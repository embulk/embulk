package org.embulk.deps.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;

@Deprecated
public final class TimestampJacksonModule extends SimpleModule {
    @SuppressWarnings("deprecation")  // For use of org.embulk.spi.time.Timestamp
    public TimestampJacksonModule() {
        this.addSerializer(org.embulk.spi.time.Timestamp.class, new TimestampSerializer());
        this.addDeserializer(org.embulk.spi.time.Timestamp.class, new TimestampDeserializer());
    }

    private static class TimestampSerializer extends JsonSerializer<org.embulk.spi.time.Timestamp> {
        @Override
        public void serialize(org.embulk.spi.time.Timestamp value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.toString());
        }
    }

    private static class TimestampDeserializer extends FromStringDeserializer<org.embulk.spi.time.Timestamp> {
        public TimestampDeserializer() {
            super(org.embulk.spi.time.Timestamp.class);
        }

        @Override
        protected org.embulk.spi.time.Timestamp _deserialize(String value, DeserializationContext context)
                throws JsonMappingException {
            if (value == null) {
                throw new JsonMappingException("TimestampDeserializer#_deserialize received null unexpectedly.");
            }
            try {
                return org.embulk.spi.time.Timestamp.ofString(value);
            } catch (final NumberFormatException ex) {
                throw new JsonMappingException("Invalid format as a Timestamp value: '" + value + "'", ex);
            } catch (final IllegalStateException ex) {
                throw new JsonMappingException("Unexpected failure in parsing: '" + value + "'", ex);
            }
        }
    }
}
