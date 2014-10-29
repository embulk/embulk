package org.quickload.record;

public interface PageAllocator
{
    public Page allocatePage(int minimumCapacity);
}
