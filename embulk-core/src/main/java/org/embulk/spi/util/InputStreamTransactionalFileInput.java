package org.embulk.spi.util;

import java.io.InputStream;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.spi.BufferAllocator;

public abstract class InputStreamTransactionalFileInput
        extends InputStreamFileInput
        implements TransactionalFileInput
{
    public InputStreamTransactionalFileInput(BufferAllocator allocator, Provider provider)
    {
        super(allocator, provider);
    }

    public InputStreamTransactionalFileInput(BufferAllocator allocator, Opener opener)
    {
        super(allocator, opener);
    }

    public InputStreamTransactionalFileInput(BufferAllocator allocator, InputStream openedStream)
    {
        super(allocator, openedStream);
    }
}
