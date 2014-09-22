package org.quickload.spi;

import org.quickload.record.Page;
import org.quickload.record.PageOutput;

public interface OutputOperator
        extends Operator, PageOutput
{
    public void addPage(Page page);
}
