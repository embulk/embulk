package org.quickload.buffer;

public class Buffer
{
    // TODO
    private byte[] buf;

    public Buffer(byte[] buf)
    {
        this.buf = buf;
    }

    public byte[] get()
    {
        return buf;
    }
}
