package org.quickload.spi;

import java.io.IOException;
import java.nio.charset.Charset;
import com.google.inject.Inject;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import org.quickload.config.ModelManager;

public class CharsetSerDe
{
    @Inject
    public CharsetSerDe(ModelManager modelManager)
    {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Charset.class, new CharsetSerializer());
        module.addDeserializer(Charset.class, new CharsetDeserializer());
        modelManager.addObjectMapperModule(module);
    }

    public static class CharsetSerializer
            extends JsonSerializer<Charset>
    {
        @Override
        public void serialize(Charset value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException
        {
            jgen.writeString(value.name());
        }
    }

    public static class CharsetDeserializer
            extends FromStringDeserializer<Charset>
    {
        public CharsetDeserializer()
        {
            super(Charset.class);
        }

        @Override
        protected Charset _deserialize(String value, DeserializationContext context)
                throws JsonMappingException
        {
            try {
                return Charset.forName(value);
            } catch (UnsupportedOperationException ex) {
                // TODO include link to a document to the message for the list of supported time zones
                throw new JsonMappingException(String.format("Unknown charset '%s'", value));
            }
        }
    }
}
