package org.quickload.channel;

import org.quickload.buffer.Buffer;

public class FileBufferOutput
        extends BufferOutput
{
    FileBufferOutput(DataChannel<Buffer> channel)
    {
        super(channel);
    }

    public void add(Buffer buffer)
    {
        channel.add(buffer);
    }

    public void addFile()
    {
        channel.add(FileBufferChannel.END_OF_FILE);
    }
}
