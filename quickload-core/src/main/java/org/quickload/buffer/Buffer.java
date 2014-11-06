package org.quickload.buffer;

import io.airlift.slice.Slices;
import io.airlift.slice.Slice;
import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;

public class Buffer
{
    private final Slice slice;
    protected int limit;

    protected Buffer(int size)
    {
        this(Slices.allocate(size));
    }

    protected Buffer(byte[] wrap)
    {
        this(Slices.wrappedBuffer(wrap, 0, wrap.length));
    }

    private Buffer(Slice slice)
    {
        this.slice = slice;
        this.limit = 0;
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
        return wrap(src, src.length);
    }

    public static Buffer wrap(byte[] src, int limit)
    {
        Buffer buffer = new Buffer(src);
        buffer.limit(limit);
        return buffer;
    }

    // TODO setXxx and getXxx methods need to add offset to pos anytime if this method exists?
    //public static Buffer wrap(byte[] src, int index, int length)
    //{
    //    Buffer buffer = new Buffer(Slices.wrappedBuffer(src, index, length));
    //    buffer.limit(length);
    //    return buffer;
    //}

    public void release()
    {
        // BufferAllocator can overwrite this method
    }

    public byte[] get()
    {
        return (byte[]) slice.getBase();
    }

    public Buffer limit(int newLimit)
    {
        this.limit = newLimit;
        return this;
    }

    public int limit()
    {
        return limit;
    }

    public int capacity()
    {
        return slice.length();
    }

    public byte getByte(int pos)
    {
        return slice.getByte(pos);
    }

    public short getShort(int pos)
    {
        return slice.getShort(pos);
    }

    public int getInt(int pos)
    {
        return slice.getInt(pos);
    }

    public long getLong(int pos)
    {
        return slice.getLong(pos);
    }

    public float getFloat(int pos)
    {
        return slice.getFloat(pos);
    }

    public double getDouble(int pos)
    {
        return slice.getDouble(pos);
    }

    public void getBytes(int pos, byte[] dst)
    {
        slice.getBytes(pos, dst, 0, dst.length);
    }

    public void getBytes(int pos, byte[] dst, int offset, int length)
    {
        slice.getBytes(pos, dst, offset, length);
    }

    public void setByte(int pos, byte value)
    {
        slice.setByte(pos, value);
    }

    public void setShort(int pos, short value)
    {
        slice.setShort(pos, value);
    }

    public void setInt(int pos, int value)
    {
        slice.setInt(pos, value);
    }

    public void setLong(int pos, long value)
    {
        slice.setLong(pos, value);
    }

    public void setFloat(int pos, float value)
    {
        slice.setFloat(pos, value);
    }

    public void setDouble(int pos, double value)
    {
        slice.setDouble(pos, value);
    }

    public void setBytes(int pos, byte[] src)
    {
        slice.setBytes(pos, src, 0, src.length);
    }

    public void setBytes(int pos, byte[] src, int srcIndex, int length)
    {
        slice.setBytes(pos, src, srcIndex, length);
    }

    public void setBytes(int pos, Buffer src, int srcIndex, int length)
    {
        slice.setBytes(pos, src.slice, srcIndex, length);
    }
}
