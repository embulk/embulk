package org.quickload.channel;

import org.quickload.buffer.Buffer;

public class FileBufferOutput
        extends BufferOutput
{
    protected FileBufferOutput(DataChannel<Buffer> channel)
    {
        super(channel);
    }

    public void addFile()
    {
        channel.add(FileBufferChannel.END_OF_FILE);
        addedSize = 0;
    }
}
