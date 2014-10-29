package org.quickload.buffer;

public interface BufferAllocator
{
    public Buffer allocateBuffer(int minimumCapacity);
}
