package org.embulk.spi.time;

import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

public class TimestampFormat
{
    private final String format;

    @JsonCreator
    public TimestampFormat(String format)
    {
        this.format = format;
    }

    @JsonValue
    public String getFormat()
    {
        return format;
    }

    @Deprecated
    public TimestampFormatter newFormatter(TimestampFormatter.FormatterTask task)
    {
        // NOTE: Its deprecation is not actually from ScriptingContainer, though.
        // TODO: Notify users about deprecated calls through the notification reporter.
        if (!newFormatterDeprecationWarned) {
            System.err.println("[WARN] Plugin uses deprecated method org.embulk.spi.time.TimestampFormat.newFormatter");
            System.err.println("[WARN] Report plugins in your config at: https://github.com/embulk/embulk/issues/828");
            // The |newFormatterDeprecationWarned| flag is used only for warning messages.
            // Even in case of race conditions, messages are just duplicated -- should be acceptable.
            newFormatterDeprecationWarned = true;
        }
        return new TimestampFormatter(format, task.getTimeZone());
    }

    @Deprecated
    public TimestampParser newParser(TimestampParser.ParserTask task)
    {
        // NOTE: Its deprecation is not actually from ScriptingContainer, though.
        // TODO: Notify users about deprecated calls through the notification reporter.
        if (!newParserDeprecationWarned) {
            System.err.println("[WARN] Plugin uses deprecated method org.embulk.spi.time.TimestampFormat.newParser");
            System.err.println("[WARN] Report plugins in your config at: https://github.com/embulk/embulk/issues/828");
            // The |newParserDeprecationWarned| flag is used only for warning messages.
            // Even in case of race conditions, messages are just duplicated -- should be acceptable.
            newParserDeprecationWarned = true;
        }
        return new TimestampParser(format, task.getDefaultTimeZone());
    }

    private static Set<String> availableTimeZoneNames = ImmutableSet.copyOf(DateTimeZone.getAvailableIDs());

    public static DateTimeZone parseDateTimeZone(String s)
    {
        final int rubyStyleTimeOffsetInSecond = TimeZoneConverter.dateZoneToDiff(s);

        if (rubyStyleTimeOffsetInSecond != Integer.MIN_VALUE) {
            return DateTimeZone.forOffsetMillis(rubyStyleTimeOffsetInSecond * 1000);
        }

        if(s.startsWith("+") || s.startsWith("-")) {
            return DateTimeZone.forID(s);

        } else if (s.equals("Z")) {
            return DateTimeZone.UTC;

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

    private static boolean newFormatterDeprecationWarned = false;
    private static boolean newParserDeprecationWarned = false;

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
