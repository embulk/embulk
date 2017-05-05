package org.embulk.spi.time;

import org.jruby.util.RubyTimeOutputFormatter;

// Ported from org.jruby.util.RubyDateFormatter.Token in JRuby
public class StrftimeToken
{
    static final StrftimeToken[] CONVERSION2TOKEN = new StrftimeToken[256];

    private final StrftimeFormat format;
    private final Object data;

    StrftimeToken(StrftimeFormat format)
    {
        this(format, null);
    }

    StrftimeToken(StrftimeFormat formatString, Object data)
    {
        this.format = formatString;
        this.data = data;
    }

    public static StrftimeToken str(String str)
    {
        return new StrftimeToken(StrftimeFormat.FORMAT_STRING, str);
    }

    public static StrftimeToken format(char c)
    {
        return CONVERSION2TOKEN[c];
    }

    public static StrftimeToken zoneOffsetColons(int colons)
    {
        return new StrftimeToken(StrftimeFormat.FORMAT_COLON_ZONE_OFF, colons);
    }

    public static StrftimeToken special(char c)
    {
        return new StrftimeToken(StrftimeFormat.FORMAT_SPECIAL, c);
    }

    public static StrftimeToken formatter(RubyTimeOutputFormatter formatter)
    {
        return new StrftimeToken(StrftimeFormat.FORMAT_OUTPUT, formatter);
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
    StrftimeFormat getFormat()
    {
        return format;
    }

    @Override
    public String toString()
    {
        return "<Token "+format+ " "+data+">";
    }
}
