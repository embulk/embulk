package org.embulk.spi;

public class AbortTransactionResource
        implements AutoCloseable
{
    private Transactional tran;

    public AbortTransactionResource()
    {
        this(null);
    }

    public AbortTransactionResource(Transactional tran)
    {
        this.tran = tran;
    }

    public void abortThis(Transactional tran)
    {
        this.tran = tran;
    }

    public void dontAbort()
    {
        this.tran = null;
    }

    @Override
    public void close()
    {
        if (tran != null) {
            tran.abort();
        }
    }
}

