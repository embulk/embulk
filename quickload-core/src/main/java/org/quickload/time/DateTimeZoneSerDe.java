package org.quickload.time;

import java.io.IOException;
import org.joda.time.DateTimeZone;
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

public class DateTimeZoneSerDe
{
    @Inject
    public DateTimeZoneSerDe(ModelManager modelManager)
    {
        SimpleModule module = new SimpleModule();
        module.addSerializer(DateTimeZone.class, new DateTimeZoneSerializer());
        module.addDeserializer(DateTimeZone.class, new DateTimeZoneDeserializer());
        modelManager.addObjectMapperModule(module);
    }

    public static class DateTimeZoneSerializer
            extends JsonSerializer<DateTimeZone>
    {
        @Override
        public void serialize(DateTimeZone value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException
        {
            jgen.writeString(value.getID());
        }
    }

    public static class DateTimeZoneDeserializer
            extends FromStringDeserializer<DateTimeZone>
    {
        public DateTimeZoneDeserializer()
        {
            super(DateTimeZone.class);
        }

        @Override
        protected DateTimeZone _deserialize(String value, DeserializationContext context)
                throws JsonMappingException
        {
            DateTimeZone parsed = TimestampFormatConfig.parseDateTimeZone(value);
            if (parsed == null) {
                // TODO include link to a document to the message for the list of supported time zones
                throw new JsonMappingException(String.format("Unknown timezone '%s'", value));
            }
            return parsed;
        }
    }
}
