package org.quickload.spi;

import org.quickload.page.Page;

public interface OutputOperator
        extends Operator
{
    public void addPage(Page page);
}
