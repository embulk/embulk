package org.quickload.exec;

import java.util.List;
import org.quickload.record.Schema;
import org.quickload.record.Page;

public class PreviewResult
{
    private final Schema schema;
    private final List<Page> pages;

    public PreviewResult(Schema schema, List<Page> pages)
    {
        this.schema = schema;
        this.pages = pages;
    }

    public Schema getSchema()
    {
        return schema;
    }

    public List<Page> getPages()
    {
        return pages;
    }
}
