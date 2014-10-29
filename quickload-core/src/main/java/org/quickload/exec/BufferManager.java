package org.quickload.exec;

import org.quickload.buffer.Buffer;
import org.quickload.buffer.BufferAllocator;
import org.quickload.record.Page;
import org.quickload.record.PageAllocator;

public class BufferManager
        implements PageAllocator, BufferAllocator
{
    @Override
    public Buffer allocateBuffer(int minimumCapacity)
    {
        return Buffer.allocate(minimumCapacity);
    }

    @Override
    public Page allocatePage(int minimumCapacity)
    {
        // TODO cache
        return Page.allocate(Math.max(128*1024, minimumCapacity));
    }
}
