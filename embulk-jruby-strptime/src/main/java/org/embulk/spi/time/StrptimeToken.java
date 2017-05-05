package org.embulk.spi.time;

import org.jruby.util.RubyTimeOutputFormatter;

// Ported from org.jruby.util.RubyDateFormatter.Token in JRuby
public class StrptimeToken
{
    static final StrptimeToken[] CONVERSION2TOKEN = new StrptimeToken[256];

    private final StrptimeFormat format;
    private final Object data;

    StrptimeToken(StrptimeFormat format)
    {
        this(format, null);
    }

    StrptimeToken(StrptimeFormat formatString, Object data)
    {
        this.format = formatString;
        this.data = data;
    }

    public static StrptimeToken str(String str)
    {
        return new StrptimeToken(StrptimeFormat.FORMAT_STRING, str);
    }

    public static StrptimeToken format(char c)
    {
        return CONVERSION2TOKEN[c];
    }

    public static StrptimeToken zoneOffsetColons(int colons)
    {
        return new StrptimeToken(StrptimeFormat.FORMAT_COLON_ZONE_OFF, colons);
    }

    public static StrptimeToken special(char c)
    {
        return new StrptimeToken(StrptimeFormat.FORMAT_SPECIAL, c);
    }

    public static StrptimeToken formatter(RubyTimeOutputFormatter formatter)
    {
        return new StrptimeToken(StrptimeFormat.FORMAT_OUTPUT, formatter);
    }

    /**
     * Gets the data.
     * @return Returns a Object
     */
    Object getData()
    {
        return data;
    }

    /**
     * Gets the format.
     * @return Returns a int
     */
    StrptimeFormat getFormat()
    {
        return format;
    }

    @Override
    public String toString()
    {
        return "<Token "+format+ " "+data+">";
    }
}
