package org.quickload.record;

public class PageReader
{
    private final RecordCursor cursor;

    public PageReader(Schema schema)
    {
        this.cursor = new RecordCursor(schema);
    }

    public RecordCursor cursor(Page page)
    {
        cursor.reset(page);
        return cursor;
    }
}
