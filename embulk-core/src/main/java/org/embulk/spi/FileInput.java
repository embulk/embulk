package org.embulk.spi;

public interface FileInput
        extends AutoCloseable
{
    public boolean nextFile();

    public Buffer poll();

    public void close();
}
