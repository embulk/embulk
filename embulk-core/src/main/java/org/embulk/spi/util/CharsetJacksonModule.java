package org.embulk.spi.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.nio.charset.Charset;

public final class CharsetJacksonModule extends SimpleModule {
    public CharsetJacksonModule() {
        this.addSerializer(Charset.class, new CharsetSerializer());
        this.addDeserializer(Charset.class, new CharsetDeserializer());
    }

    private static class CharsetSerializer extends JsonSerializer<Charset> {
        @Override
        public void serialize(Charset value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.name());
        }
    }

    private static class CharsetDeserializer extends FromStringDeserializer<Charset> {
        public CharsetDeserializer() {
            super(Charset.class);
        }

        @Override
        protected Charset _deserialize(String value, DeserializationContext context)
                throws JsonMappingException {
            try {
                return Charset.forName(value);
            } catch (UnsupportedOperationException ex) {
                // TODO include link to a document to the message for the list of supported time zones
                throw new JsonMappingException(String.format("Unknown charset '%s'", value));
            }
        }
    }
}
