package org.quickload.spi;

public abstract class AbstractOperator <O extends Operator>
        implements Operator
{
    protected O next;

    public AbstractOperator(O next)
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
