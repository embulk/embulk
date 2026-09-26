package org.embulk.spi.unit;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import org.embulk.spi.time.Timestamp;

public class TimestampParam
{
    private final Timestamp timestamp;

    private TimestampParam(Timestamp timestamp)
    {
        this.timestamp = timestamp;
    }

    public Timestamp getTimestamp()
    {
        return timestamp;
    }

    @JsonCreator
    public static TimestampParam fromJson(JsonNode node)
        throws JsonMappingException
    {
        String url;
        if (node.canConvertToLong()) {
            return of(Timestamp.ofEpochSecond(node.asLong()));
        }
        else if (node.isTextual()) {
            try {
                return fromString(node.asText());
            }
            catch (IllegalArgumentException ex) {
                throw new JsonMappingException(ex.getMessage());
            }
        }
        else {
            throw new JsonMappingException("Can not deserialize instance of TimestampParam out of malformed object: " + node);

        }
    }

    public static TimestampParam fromString(String timestamp)
    {
        Date date;
        try {
            if (timestamp.contains("UTC")) {
                if (timestamp.indexOf('.') >= 0) {
                    // with fractional seconds
                    date = pattern("yyyy-MM-dd HH:mm:ss.SSS 'UTC'").parse(timestamp);
                }
                else {
                    date = pattern("yyyy-MM-dd HH:mm:ss 'UTC'").parse(timestamp);
                }
            }
            else if (timestamp.indexOf('T') >= 0) {
                // ISO 8601 & RFC 3339 format
                if (timestamp.indexOf(' ') >= 0) {
                    throw new IllegalArgumentException("Invalid timestamp format: " + timestamp);
                }
                if (timestamp.indexOf('.') >= 0) {
                    // with fractional seconds
                    date = pattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse(timestamp);
                }
                else {
                    date = pattern("yyyy-MM-dd'T'HH:mm:ssX").parse(timestamp);
                }
            }
            else {
                if (timestamp.indexOf('.') >= 0) {
                    // with fractional seconds
                    date = pattern("yyyy-MM-dd HH:mm:ss.SSS X").parse(timestamp);
                }
                else {
                    date = pattern("yyyy-MM-dd HH:mm:ss X").parse(timestamp);
                }
            }
        }
        catch (ParseException ex) {
            throw new IllegalArgumentException("Invalid timestamp format: " + timestamp + " (" + ex.getMessage() + ")", ex);
        }
        return of(Timestamp.ofEpochMilli(date.getTime()));
    }

    public static TimestampParam of(Timestamp timestamp)
    {
        return new TimestampParam(timestamp);
    }

    @JsonValue
    @Override
    public String toString()
    {
        long millis = timestamp.toEpochMilli();
        if (millis % 1000 == 0) {
            return pattern("yyyy-MM-dd'T'HH:mm:ssX").format(new Date(millis));
        }
        else {
            return pattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").format(new Date(millis));
        }
    }

    private static SimpleDateFormat pattern(String pattern)
    {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }
}
