package org.quickload.spi;

import org.quickload.record.Page;

public abstract class AbstractOutputOperator
        implements OutputOperator
{
    public AbstractOutputOperator()
    {
    }

    @Override
    public abstract void addPage(Page page);

    @Override
    public Report failed(Exception cause)
    {
        return new FailedReport(null, null);
    }

    @Override
    public abstract Report completed();

    @Override
    public abstract void close() throws Exception;
}
