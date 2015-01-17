package org.embulk.standards;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

// @see http://linux.die.net/man/3/strftime
public class StrftimeFormat
{
    private final Map<String, String> mapping;

    public StrftimeFormat()
    {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();

        //  %a The abbreviated weekday name according to the current locale.
        builder.put("a", "EEE");

        //  %A The full weekday name according to the current locale.
        builder.put("A", "EEEE");

        //  %b The abbreviated month name according to the current locale.
        builder.put("b", "MMM");

        //  %B The full month name according to the current locale.
        builder.put("B", "MMMM");

        //  %c The preferred date and time representation for the current locale.
        builder.put("c", "EEE MMM d HH:mm:ss yyyy");

        //  %C The century number (year/100) as a 2-digit integer. (SU)
        //  Not supported

        //  %d The day of the month as a decimal number (range 01 to 31).
        builder.put("d", "dd");

        //  %D Equivalent to %m/%d/%y. (Yecch-for Americans only. Americans should note that in other countries
        //     %d/%m/%y is rather common. This means that in international context this format is ambiguous and
        //     should not be used.) (SU)
        builder.put("D", "MM/dd/yy");

        //  %e Like %d, the day of the month as a decimal number, but a leading zero is replaced by a space. (SU)
        builder.put("e", "dd");

        //  %E Modifier: use alternative format, see below. (SU)
        //  Not supported

        //  %F Equivalent to %Y-%m-%d (the ISO 8601 date format). (C99)
        builder.put("F", "yyyy-MM-dd");

        //  %G The ISO 8601 week-based year (see NOTES) with century as a decimal number. The 4-digit year
        //     corresponding to the ISO week number (see %V). This has the same format and value as %Y, except that
        //     if the ISO week number belongs to the previous or next year, that year is used instead. (TZ)
        builder.put("G", "yyyy");

        //  %g Like %G, but without century, that is, with a 2-digit year (00-99). (TZ)
        builder.put("g", "yy");

        //  %h Equivalent to %b. (SU)
        builder.put("h", "MMM");

        //  %H The hour as a decimal number using a 24-hour clock (range 00 to 23).
        builder.put("H", "HH");

        //  %I The hour as a decimal number using a 12-hour clock (range 01 to 12).
        builder.put("I", "hh");

        //  %j The day of the year as a decimal number (range 001 to 366).
        builder.put("j", "DDD");

        //  %k The hour (24-hour clock) as a decimal number (range 0 to 23); single digits are preceded by a blank.
        //     (See also %H.) (TZ)
        builder.put("k", "HH"); // will show as '07' instead of ' 7'

        //  %l The hour (12-hour clock) as a decimal number (range 1 to 12); single digits are preceded by a blank.
        //     (See also %I.) (TZ)
        builder.put("l", "hh"); //will show as '07' instead of ' 7'

        //  %m The month as a decimal number (range 01 to 12).
        builder.put("m", "MM");

        //  %M The minute as a decimal number (range 00 to 59).
        builder.put("M", "mm");

        //  %n A newline character. (SU)
        builder.put("n", "\n");

        //  %O Modifier: use alternative format, see below. (SU)
        //  Not supported

        //  %p Either "AM" or "PM" according to the given time value, or the corresponding strings for the current
        //     locale. Noon is treated as "PM" and midnight as "AM".
        builder.put("p", "a");

        //  %P Like %p but in lowercase: "am" or "pm" or a corresponding string for the current locale. (GNU)
        builder.put("P","a");

        //  %r The time in a.m. or p.m. notation. In the POSIX locale this is equivalent to %I:%M:%S %p. (SU)
        builder.put("r", "hh:mm:ss a");

        //  %R The time in 24-hour notation (%H:%M). (SU) For a version including the seconds, see %T below.
        builder.put("R","HH:mm");

        //  %s The number of seconds since the Epoch, 1970-01-01 00:00:00 +0000 (UTC). (TZ)
        //  Not supported

        //  %S The second as a decimal number (range 00 to 60). (The range is up to 60 to allow for occasional leap
        //     seconds.)
        builder.put("S", "ss");

        //  %t A tab character. (SU)
        builder.put("t", "\t");

        //  %T The time in 24-hour notation (%H:%M:%S). (SU)
        builder.put("T", "HH:mm:ss");

        //  %u The day of the week as a decimal, range 1 to 7, Monday being 1. See also %w. (SU)
        //  Not supported

        //  %U The week number of the current year as a decimal number, range 00 to 53, starting with the first Sunday
        //     as the first day of week 01. See also %V and %W.
        //  Not supported

        //  %V The ISO 8601 week number (see NOTES) of the current year as a decimal number, range 01 to 53, where week
        //     1 is the first week that has at least 4 days in the new year. See also %U and %W. (SU)
        builder.put("V", "ww"); //I'm not sure this is always exactly the same

        //  %w The day of the week as a decimal, range 0 to 6, Sunday being 0. See also %u.
        //  Not supported

        //  %W The week number of the current year as a decimal number, range 00 to 53, starting with the first Monday
        //     as the first day of week 01.
        //  Not supported

        //  %x The preferred date representation for the current locale without the time.
        builder.put("x", "MM/dd/yy");

        //  %X The preferred time representation for the current locale without the date.
        builder.put("X", "HH:mm:ss");

        //  %y The year as a decimal number without a century (range 00 to 99).
        builder.put("y", "yy");

        //  %Y The year as a decimal number including the century.
        builder.put("Y", "yyyy");

        //  %z The +hhmm or -hhmm numeric timezone (that is, the hour and minute offset from UTC). (SU)
        builder.put("z", "Z");

        //  %Z The timezone or name or abbreviation.
        builder.put("Z", "z");

        //  %+ The date and time in date(1) format. (TZ) (Not supported in glibc2.)
        //  Not supported

        //  %% A literal '%' character.
        builder.put("%", "%");

        mapping = builder.build();
    }

    public String toSimpleDateFormat(String format)
    {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        while (i < format.length()) {
            char c = format.charAt(i++);
            if (c == '%') {
                char next = format.charAt(i++);
                String v = mapping.get(String.valueOf((char)next));
                if (v == null) {
                    throw new UnsupportedOperationException("Not support: '%"+((char)next)+"'");
                }
                sb.append(v);

            } else {
                sb.append(c);

            }
        }

        return sb.toString();
    }
}
