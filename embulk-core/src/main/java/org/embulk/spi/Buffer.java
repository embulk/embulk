package org.embulk.spi;

public class Buffer
{
    private byte[] array;
    private int offset;
    private int filled;

    protected Buffer(byte[] wrap, int offset, int size)
    {
        this.array = wrap;
        this.offset = offset;
        this.filled = offset + size;
    }

    public static Buffer allocate(int length)
    {
        return new Buffer(length);
    }

    public static Buffer copyOf(byte[] src)
    {
        return copyOf(src, 0, src.length);
    }

    public static Buffer copyOf(byte[] src, int index, int length)
    {
        Buffer buffer = allocate(length);
        buffer.setBytes(0, src, index, length);
        return buffer.limit(length);
    }

    public static Buffer wrap(byte[] src)
    {
        return wrap(src, 0, src.length);
    }

    public static Buffer wrap(byte[] src, int offset, int size)
    {
        return new Buffer(src, offset, size);
    }

    public byte[] array()
    {
        return array;
    }

    public int offset()
    {
        return offset;
    }

    public Buffer offset(int offset)
    {
        this.offset = offset;
        return this;
    }

    public int size()
    {
        return filled - offset;
    }

    public Buffer size(int size)
    {
        this.filled = offset + size;
        return this;
    }

    public int capacity()
    {
        return array.length - offset;
    }

    public void release()
    {
    }
}
