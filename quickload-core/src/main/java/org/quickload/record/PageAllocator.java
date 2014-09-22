package org.quickload.record;

public interface PageAllocator
{
    public Page allocatePage(int minimumCapacity);

    public void releasePage(Page page);
}
