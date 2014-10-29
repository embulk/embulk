package org.quickload.channel;

import org.quickload.buffer.Buffer;

public class BufferOutput
{
    private final DataChannel<Buffer> channel;

    BufferOutput(DataChannel<Buffer> channel)
    {
        this.channel = channel;
    }

    public void add(Buffer buffer)
    {
        channel.add(buffer);
    }
}
