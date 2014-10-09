package org.quickload.spi;

public interface FileInputTask
        extends InputTask
{
    public String getConfigExpression();

    public ParserTask getParserTask();
}
