package org.embulk.spi;

public interface ExecAction <T>
{
    public T run() throws Exception;
}
