package org.quickload.buffer;

import java.nio.ByteBuffer;

public class Buffer
        implements Allocated
{
    public static Buffer allocate(int size) {
        return new Buffer(ByteBuffer.allocate(size));
    }

    private ByteBuffer bb;

    public Buffer(ByteBuffer bb) {
        this.bb = bb;
    }

    public int length()
    {
        return bb.remaining();
    }

    public int capacity()
    {
        return bb.capacity();
    }

    public void write(byte[] bytes, int index, int len)
    {
        bb.put(bytes, index, len);
    }

    public ByteBuffer getBuffer()
    {
        return bb;
    }

    public void flush()
    {
        bb.flip();
    }

    public void release()
    {
    }
}
