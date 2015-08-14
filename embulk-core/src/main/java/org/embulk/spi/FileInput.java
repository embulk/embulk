package org.embulk.spi;

public interface FileInput
        extends AutoCloseable
{
    boolean nextFile();

    Buffer poll();

    void close();
}
