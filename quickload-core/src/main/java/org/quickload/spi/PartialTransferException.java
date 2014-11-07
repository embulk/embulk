package org.quickload.standards;

public class PartialTransferException
        extends RuntimeException
{
    private long transferredSize;

    public PartialTransferException(Exception cause, long transferredSize)
    {
        super(cause);
        this.transferredSize = transferredSize;
    }

    public Exception getCause()
    {
        return (Exception) super.getCause();
    }

    public long getTransferredSize()
    {
        return transferredSize;
    }
}
