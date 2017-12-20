package org.embulk.spi.time;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Set;

public class TimestampFormat
{
    @JsonCreator
    public TimestampFormat(final String format)
    {
        this.format = format;
    }

    @JsonValue
    public String getFormat()
    {
        return this.format;
    }

    // To be deprecated.
    public static org.joda.time.DateTimeZone parseDateTimeZone(final String timeZoneName)
    {
        return TimeZoneIds.parseJodaDateTimeZone(timeZoneName);
    }

    private final String format;
}
