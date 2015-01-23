package org.embulk.spi.time;

public interface JRubyTimeParserHelper
{
    public long strptime(String text) throws TimestampParseException;

    public String getZone();
}
