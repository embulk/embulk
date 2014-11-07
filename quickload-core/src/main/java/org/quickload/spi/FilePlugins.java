package org.quickload.standards;

import java.io.InputStream;
import org.quickload.buffer.BufferAllocator;
import org.quickload.channel.FileBufferOutput;

public class FilePlugins
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
