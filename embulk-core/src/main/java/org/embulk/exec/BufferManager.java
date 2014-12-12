package org.embulk.exec;

import org.embulk.buffer.Buffer;
import org.embulk.buffer.BufferAllocator;
import org.embulk.record.Page;
import org.embulk.record.PageAllocator;

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
