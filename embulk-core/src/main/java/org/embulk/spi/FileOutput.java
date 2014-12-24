package org.embulk.spi;

public interface FileOutput
        extends AutoCloseable
{
    public void nextFile();

    public void add(Buffer buffer);

    public void finish();

    public void close();
}
