package org.embulk.spi;

public interface FileOutput
        extends AutoCloseable
{
    void nextFile();

    void add(Buffer buffer);

    void finish();

    void close();
}
