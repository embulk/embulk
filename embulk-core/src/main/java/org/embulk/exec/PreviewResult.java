package org.embulk.exec;

import java.util.List;
import org.embulk.spi.Schema;
import org.embulk.spi.Page;

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
