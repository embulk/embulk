package org.quickload.channel;

import org.quickload.buffer.Buffer;

public class BufferOutput
{
    protected final DataChannel<Buffer> channel;
    protected long addedSize;

    BufferOutput(DataChannel<Buffer> channel)
    {
        this.channel = channel;
    }

    public void add(Buffer buffer)
    {
        channel.add(buffer);
        addedSize += buffer.length();
    }

    public long getAddedSize()
    {
        return addedSize;
    }
}
