package org.embulk.exec;

import java.util.Deque;
import java.util.ArrayDeque;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Page;
import org.embulk.spi.PageAllocator;

public class BufferManager
        implements PageAllocator, BufferAllocator
{
    private final Deque<Buffer> buffers = new ArrayDeque<Buffer>();
    private final Deque<Page> pages = new ArrayDeque<Page>();

    @Override
    public synchronized Buffer allocateBuffer(int minimumCapacity)
    {
        return Buffer.allocate(Math.max(128*1024, minimumCapacity));
        /*
        if (!buffers.isEmpty()) {
            return buffers.removeFirst();
        }
        return new Buffer(Math.max(128*1024, minimumCapacity)) {
            private boolean released = false;
            public void release()
            {
                if (!released) {
                    synchronized(BufferManager.this) {
                        buffers.add(this);
                    }
                    released = true;
                }
            }
        };
        */
    }

    @Override
    public synchronized Page allocatePage(int minimumCapacity)
    {
        return Page.allocate(Math.max(128*1024, minimumCapacity));
        /*
        if (!pages.isEmpty()) {
            return pages.removeFirst();
        }
        return new Page(Math.max(128*1024, minimumCapacity)) {
            private boolean released = false;
            public void release()
            {
                if (!released) {
                    synchronized(BufferManager.this) {
                        pages.add(this);
                    }
                    released = true;
                }
            }
        };
        */
    }
}
