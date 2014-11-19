package org.quickload.time;

import java.util.Set;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import com.google.common.collect.ImmutableSet;
import com.fasterxml.jackson.annotation.JsonValue;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.jruby.embed.ScriptingContainer;

public class TimestampFormatConfig
{
    private final ScriptingContainer jruby;
    private final String format;

    public TimestampFormatConfig(ScriptingContainer jruby, String format)
    {
        this.jruby = jruby;
        this.format = format;
    }

    @JsonValue
    public String getFormat()
    {
        return format;
    }

    public TimestampFormatter newFormatter(TimestampFormatterTask task)
    {
        return new TimestampFormatter(jruby, format, task);
    }

    public TimestampParser newParser(TimestampParserTask task)
    {
        return new TimestampParser(jruby, format, task);
    }

    private static Set<String> availableTimeZoneNames = ImmutableSet.copyOf(DateTimeZone.getAvailableIDs());

    static DateTimeZone parseDateTimeZone(String s)
    {
        if(s.startsWith("+") || s.startsWith("-")) {
            return DateTimeZone.forID("GMT"+s);

        } else {
            try {
                int rawOffset = (int) DateTimeFormat.forPattern("z").parseMillis(s);
                if(rawOffset == 0) {
                    return DateTimeZone.UTC;
                }
                int offset = rawOffset / -1000;
                int h = offset / 3600;
                int m = offset % 3600;
                return DateTimeZone.forOffsetHoursMinutes(h, m);
            } catch (IllegalArgumentException ex) {
                // parseMillis failed
            }

            // TimeZone.getTimeZone returns GMT zone if given timezone id is not found
            // we want to only return timezone if exact match, otherwise exception
            if (availableTimeZoneNames.contains(s)) {
                //return TimeZone.getTimeZone(s);
                return DateTimeZone.forID(s);
            }
            return null;
        }
    }

    //// Java standard TimeZone
    //static TimeZone parseDateTimeZone(String s)
    //{
    //    if(s.startsWith("+") || s.startsWith("-")) {
    //        return TimeZone.getTimeZone("GMT"+s);
    //
    //    } else {
    //        ParsePosition pp = new ParsePosition(0);
    //        Date off = new SimpleDateFormat("z").parse(s, pp);
    //        if(off != null && pp.getErrorIndex() == -1) {
    //            int rawOffset = (int) off.getTime();
    //            if(rawOffset == 0) {
    //                return TimeZone.UTC;
    //            }
    //            int offset = rawOffset / -1000;
    //            int h = offset / 3600;
    //            int m = offset % 3600;
    //            return DateTimeZone.getTimeZone(String.format("GMT%+02d%02d", h, m));
    //        }
    //
    //        // TimeZone.getTimeZone returns GMT zone if given timezone id is not found
    //        // we want to only return timezone if exact match, otherwise exception
    //        if (availableTimeZoneNames.contains(s)) {
    //            return TimeZone.getTimeZone(s);
    //        }
    //        return null;
    //    }
    //}
}
