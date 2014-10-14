package org.quickload.spi;

public abstract class AbstractOperator <NextOperator extends Operator>
        implements Operator
{
    protected NextOperator next;

    public AbstractOperator(NextOperator next)
    {
        this.next = next;
    }

    public Report failed(Exception cause)
    {
        return next.failed(cause);
    }

    public Report completed()
    {
        return next.completed();
    }

    public void close() throws Exception
    {
        next.close();
    }
}
