package org.quickload.exec;

import org.quickload.buffer.Buffer;
import org.quickload.record.Page;
import org.quickload.record.PageAllocator;

public class BufferManager
        implements PageAllocator
{
    public Page allocatePage(int minimumCapacity)
    {
        // TODO cache
        return Page.allocate(Math.max(128*1024, minimumCapacity));
    }

    public void releasePage(Page page)
    {
        // TODO cache
        //page.clear();
    }

    public Buffer allocateBuffer(int minimumCapacity)
    {
        return Buffer.allocate(minimumCapacity);
    }

    public void releaseBuffer(Buffer buf)
    {
        // TODO
    }
}
