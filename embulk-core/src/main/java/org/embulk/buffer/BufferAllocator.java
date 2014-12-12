package org.embulk.buffer;

public interface BufferAllocator
{
    public Buffer allocateBuffer(int minimumCapacity);
}
