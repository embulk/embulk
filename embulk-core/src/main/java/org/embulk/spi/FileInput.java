package org.embulk.spi;

public interface FileInput
        extends AutoCloseable
{
    public Buffer poll();

    public boolean nextFile();

    public void close();
}
