package org.quickload.spi;

import org.quickload.record.Page;
import org.quickload.record.PageOutput;

public interface PageOperator
        extends Operator, PageOutput
{
    public void addPage(Page page);
}
