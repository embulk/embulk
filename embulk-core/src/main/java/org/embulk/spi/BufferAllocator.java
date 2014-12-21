package org.embulk.spi;

public interface BufferAllocator
{
    public Buffer allocate();

    public Buffer allocate(int minimumCapacity);
}
