package org.quickload.channel;

import java.util.Iterator;
import org.quickload.buffer.Buffer;

public class BufferInput
        implements Iterable<Buffer>
{
    private final DataChannel<Buffer> channel;

    BufferInput(DataChannel<Buffer> channel)
    {
        this.channel = channel;
    }

    public Buffer poll()
    {
        return channel.poll();
    }

    @Override
    public Iterator<Buffer> iterator()
    {
        return channel.iterator();
    }
}
