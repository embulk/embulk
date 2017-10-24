package org.embulk.spi.time;

public interface JRubyTimeParserHelper
{
    long strptimeUsec(String text) throws TimestampParseException;

    String getZone();
}
