package org.quickload.spi;

public interface FileOutputTask
        extends OutputTask
{
    public String getFormatterType();

    public FormatterTask getFormatterTask();
}
