package org.embulk.spi.time;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;

@Deprecated
public final class DateTimeZoneJacksonModule extends SimpleModule {
    public DateTimeZoneJacksonModule() {
        this.addSerializer(org.joda.time.DateTimeZone.class, new DateTimeZoneSerializer());
        this.addDeserializer(org.joda.time.DateTimeZone.class, new DateTimeZoneDeserializer());
    }

    private static class DateTimeZoneSerializer extends JsonSerializer<org.joda.time.DateTimeZone> {
        @Override
        public void serialize(org.joda.time.DateTimeZone value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.getID());
        }
    }

    private static class DateTimeZoneDeserializer extends FromStringDeserializer<org.joda.time.DateTimeZone> {
        public DateTimeZoneDeserializer() {
            super(org.joda.time.DateTimeZone.class);
        }

        @Override
        protected org.joda.time.DateTimeZone _deserialize(String value, DeserializationContext context)
                throws JsonMappingException {
            org.joda.time.DateTimeZone parsed = TimeZoneIds.parseJodaDateTimeZone(value);
            if (parsed == null) {
                // TODO include link to a document to the message for the list of supported time zones
                throw new JsonMappingException(String.format("Unknown time zone name '%s'", value));
            }
            return parsed;
        }
    }
}
