package org.quickload.spi;

public interface FileInputTask
        extends InputTask
{
    public String getParserType();

    public ParserTask getParserTask();
}
