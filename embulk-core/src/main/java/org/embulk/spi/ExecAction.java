package org.embulk.spi;

public interface ExecAction <T>
{
    T run() throws Exception;
}
