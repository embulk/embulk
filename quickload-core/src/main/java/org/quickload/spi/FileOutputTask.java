package org.quickload.spi;

public interface FileOutputTask
        extends OutputTask
{
    public String getConfigExpression();

    public FormatterTask getFormatterTask();
}
