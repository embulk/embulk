package org.quickload.spi;

public interface InputProcessor
{
    public void cancel();

    public Report join() throws InterruptedException;

    public InputProgress getProgress();

    public void close() throws Exception;
}
