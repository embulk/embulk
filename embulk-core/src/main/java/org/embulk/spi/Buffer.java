package org.embulk.spi;

import java.util.Arrays;

public class Buffer
{
    private byte[] array;
    private int offset;
    private int filled;

    protected Buffer(byte[] wrap, int offset, int limit)
    {
        this.array = wrap;
        this.offset = offset;
        this.filled = offset + limit;
    }

    public static Buffer allocate(int length)
    {
        return new Buffer(new byte[length], 0, length);
    }

    public static Buffer copyOf(byte[] src)
    {
        return copyOf(src, 0, src.length);
    }

    public static Buffer copyOf(byte[] src, int index, int length)
    {
        return wrap(Arrays.copyOfRange(src, index, length));
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

    public int limit()
    {
        return filled - offset;
    }

    public Buffer limit(int limit)
    {
        this.filled = offset + limit;
        return this;
    }

    public int capacity()
    {
        return array.length - offset;
    }

    public void setBytes(int index, byte[] source, int sourceIndex, int length)
    {
        System.arraycopy(source, sourceIndex, array, offset + index, length);
    }

    public void getBytes(int index, byte[] dest, int destIndex, int length)
    {
        System.arraycopy(array, offset + index, dest, destIndex, length);
    }

    public void release()
    {
    }
}
