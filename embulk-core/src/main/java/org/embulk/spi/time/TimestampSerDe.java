package org.embulk.spi.time;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
            return createTimestampFromString(value);
        }
    }

    static Timestamp createTimestampFromStringForTesting(final String string)
    {
        return createTimestampFromString(string);
    }

    private static Timestamp createTimestampFromString(final String string)
    {
        // TODO: Handle exceptions.
        final Matcher matcher = TIMESTAMP_PATTERN.matcher(string);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Invalid timestamp format '%s'", string));
        }

        final long epochSecond = LocalDateTime.parse(matcher.group(1), FORMATTER).toEpochSecond(ZoneOffset.UTC);

        final String fraction = matcher.group(2);
        final int nanoAdjustment;
        if (fraction == null) {
            nanoAdjustment = 0;
        } else {
            nanoAdjustment = Integer.parseInt(fraction) * (int) Math.pow(10, 9 - fraction.length());
        }

        return Timestamp.ofEpochSecond(epochSecond, nanoAdjustment);
    }

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private static final Pattern TIMESTAMP_PATTERN =
        Pattern.compile("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(?:\\.(\\d{1,9}))? (?:UTC|\\+?00\\:?00)");
}
