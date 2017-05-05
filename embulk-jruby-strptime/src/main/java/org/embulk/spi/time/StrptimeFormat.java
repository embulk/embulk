package org.embulk.spi.time;

// Ported from org.jruby.util.RubyDateFormatter.Format in JRuby
enum StrptimeFormat
{
    /** encoding to give to output */
    FORMAT_ENCODING,
    /** raw string, no formatting */
    FORMAT_STRING,
    /** formatter */
    FORMAT_OUTPUT,
    /** composition of other formats, or depends on library */
    FORMAT_SPECIAL,

    /** %A */
    FORMAT_WEEK_LONG('A'),
    /** %a */
    FORMAT_WEEK_SHORT('a'),
    /** %B */
    FORMAT_MONTH_LONG('B'),
    /** %b, %h */
    FORMAT_MONTH_SHORT('b', 'h'),
    /** %C */
    FORMAT_CENTURY('C'),
    /** %d */
    FORMAT_DAY('d'),
    /** %e */
    FORMAT_DAY_S('e'),
    /** %G */
    FORMAT_WEEKYEAR('G'),
    /** %g */
    FORMAT_WEEKYEAR_SHORT('g'),
    /** %H */
    FORMAT_HOUR('H'),
    /** %I */
    FORMAT_HOUR_M('I'),
    /** %j */
    FORMAT_DAY_YEAR('j'),
    /** %k */
    FORMAT_HOUR_BLANK('k'),
    /** %L */
    FORMAT_MILLISEC('L'),
    /** %l */
    FORMAT_HOUR_S('l'),
    /** %M */
    FORMAT_MINUTES('M'),
    /** %m */
    FORMAT_MONTH('m'),
    /** %N */
    FORMAT_NANOSEC('N'),
    /** %P */
    FORMAT_MERIDIAN_LOWER_CASE('P'),
    /** %p */
    FORMAT_MERIDIAN('p'),
    /** %S */
    FORMAT_SECONDS('S'),
    /** %s */
    FORMAT_EPOCH('s'),
    /** %U */
    FORMAT_WEEK_YEAR_S('U'),
    /** %u */
    FORMAT_DAY_WEEK2('u'),
    /** %V */
    FORMAT_WEEK_WEEKYEAR('V'),
    /** %W */
    FORMAT_WEEK_YEAR_M('W'),
    /** %w */
    FORMAT_DAY_WEEK('w'),
    /** %Y */
    FORMAT_YEAR_LONG('Y'),
    /** %y */
    FORMAT_YEAR_SHORT('y'),
    /** %z, %:z, %::z, %:::z */
    FORMAT_COLON_ZONE_OFF, // must be given number of colons as data

    /* Change between Time and Date */
    /** %Z */
    FORMAT_ZONE_ID,

        /* Only for Date/DateTime from here */
    /** %Q */
    FORMAT_MICROSEC_EPOCH;

    private StrptimeFormat()
    {
    }

    private StrptimeFormat(char conversion)
    {
        StrptimeToken.CONVERSION2TOKEN[conversion] = new StrptimeToken(this);
    }

    private StrptimeFormat(char conversion, char alias)
    {
        this(conversion);
        StrptimeToken.CONVERSION2TOKEN[alias] = StrptimeToken.CONVERSION2TOKEN[conversion];
    }
}
