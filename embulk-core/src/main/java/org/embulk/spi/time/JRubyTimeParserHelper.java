package org.embulk.spi.time;

public interface JRubyTimeParserHelper
{
    public long strptimeUsec(String text) throws TimestampParseException;

    public String getZone();
}
