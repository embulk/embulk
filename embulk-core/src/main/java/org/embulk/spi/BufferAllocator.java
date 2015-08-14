package org.embulk.spi;

public interface BufferAllocator
{
    Buffer allocate();

    Buffer allocate(int minimumCapacity);
}
