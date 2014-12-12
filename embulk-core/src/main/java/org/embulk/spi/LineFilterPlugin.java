package org.embulk.spi;

public interface LineFilterPlugin
{
    public String filterLine(String line);
}
