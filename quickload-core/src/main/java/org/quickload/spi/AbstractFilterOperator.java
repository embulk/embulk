package org.quickload.spi;

import org.quickload.page.Page;

public abstract class AbstractFilterOperator
        implements OutputOperator
{
    protected final OutputOperator next;

    public AbstractFilterOperator(OutputOperator next)
    {
        this.next = next;
    }

    @Override
    public abstract void addPage(Page page);

    @Override
    public Report failed(Exception cause)
    {
        return next.failed(cause);
    }

    @Override
    public Report completed()
    {
        final Report nextReport = next.completed();

        // TODO serializable DynamicFilterReport
        return new Report() {
            public Report getNextReport()
            {
                return nextReport;
            }
        };
    }

    @Override
    public abstract void close() throws Exception;
}
