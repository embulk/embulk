package org.quickload.channel;

import org.quickload.buffer.Buffer;

public class BufferOutput
{
    protected final DataChannel<Buffer> channel;
    protected long addedSize;

    protected BufferOutput(DataChannel<Buffer> channel)
    {
        this.channel = channel;
    }

    public void add(Buffer buffer)
    {
        channel.add(buffer);
        addedSize += buffer.limit();
    }

    public long getAddedSize()
    {
        return addedSize;
    }
}
