package org.embulk.spi.time;

public interface JRubyTimeParserHelperFactory
{
    JRubyTimeParserHelper newInstance(String formatString, int year, int mon, int day, int hour, int min, int sec, int usec);
}
