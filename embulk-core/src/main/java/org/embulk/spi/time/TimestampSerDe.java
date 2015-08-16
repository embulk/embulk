package org.embulk.spi.time;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;

public class TimestampSerDe
{
    public static void configure(ObjectMapperModule mapper)
    {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Timestamp.class, new DateTimeZoneSerializer());
        module.addDeserializer(Timestamp.class, new DateTimeZoneDeserializer());
        mapper.registerModule(module);
    }

    public static class DateTimeZoneSerializer
            extends JsonSerializer<Timestamp>
    {
        @Override
        public void serialize(Timestamp value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException
        {
            jgen.writeString(value.toString());
        }
    }

    public static class DateTimeZoneDeserializer
            extends FromStringDeserializer<Timestamp>
    {
        public DateTimeZoneDeserializer()
        {
            super(Timestamp.class);
        }

        @Override
        protected Timestamp _deserialize(String value, DeserializationContext context)
                throws JsonMappingException
        {
            return Timestamp.fromString(value);
        }
    }
}
