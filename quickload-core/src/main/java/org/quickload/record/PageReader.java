package org.quickload.record;

public class PageReader
{
    private final RecordCursor cursor;

    public PageReader(PageAllocator allocator, Schema schema)
    {
        this.cursor = new RecordCursor(allocator, schema);
    }

    public RecordCursor cursor(Page page)
    {
        cursor.reset(page);
        return cursor;
    }
}
