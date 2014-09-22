package org.quickload.record;

public class PageBuilder
{
    private final RecordBuilder builder;

    public PageBuilder(PageAllocator allocator, Schema schema, PageOutput output)
    {
        this.builder = new RecordBuilder(allocator, schema, output);
    }

    public RecordBuilder builder()
    {
        return builder;
    }
}
