package org.embulk.spi;

public interface FileOutput
        extends AutoCloseable
{
    public void add(Buffer buffer);

    public void nextFile();

    public void finish();

    public void close();
}
