package org.embulk.record;

public interface PageAllocator
{
    public Page allocatePage(int minimumCapacity);
}
