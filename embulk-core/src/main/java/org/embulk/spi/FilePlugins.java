package org.embulk.spi;

import java.io.InputStream;
import org.embulk.buffer.BufferAllocator;
import org.embulk.channel.FileBufferOutput;

public abstract class FilePlugins
{
    /**
     * Calls BufferPlugins.transferInputStream. If it completes successfully,
     * calls fileBufferOutput.addFile().
     */
    public static long transferInputStream(BufferAllocator bufferAllocator,
            InputStream input, FileBufferOutput output) throws PartialTransferException
    {
        long transferredSize = BufferPlugins.transferInputStream(
                bufferAllocator, input, output);
        output.addFile();
        return transferredSize;
    }
}
