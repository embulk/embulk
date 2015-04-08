package org.embulk.spi;

import java.util.Arrays;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Buffer
{
    public static final Buffer EMPTY = Buffer.allocate(0);

    private final byte[] array;
    private int offset;
    private int filled;
    private final int capacity;

    protected Buffer(byte[] wrap, int offset, int capacity)
    {
        this.array = wrap;
        this.offset = offset;
        this.capacity = capacity;
        this.filled = offset;
        if (array.length < offset + capacity) {
            // TODO
            throw new IllegalStateException("capacity out of bound");
        }
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
        return new Buffer(src, offset, size).limit(size);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP")
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
        if (capacity < limit) {
            // TODO
            throw new IllegalStateException("limit index out of bound: capacity="+capacity+" limit="+limit);
        }
        this.filled = offset + limit;
        return this;
    }

    public int capacity()
    {
        return capacity;
    }

    public void setBytes(int index, byte[] source, int sourceIndex, int length)
    {
        System.arraycopy(source, sourceIndex, array, offset + index, length);
    }

    public void setBytes(int index, Buffer source, int sourceIndex, int length)
    {
        setBytes(index, source.array(), source.offset() + sourceIndex, length);
    }

    public void getBytes(int index, byte[] dest, int destIndex, int length)
    {
        System.arraycopy(array, offset + index, dest, destIndex, length);
    }

    public void getBytes(int index, Buffer dest, int destIndex, int length)
    {
        getBytes(index, dest.array(), dest.offset() + destIndex, length);
    }

    public void release()
    {
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Buffer)) {
            return false;
        }
        Buffer o = (Buffer) other;

        // TODO optimize
        if (limit() != o.limit()) {
            return false;
        }
        int i = offset;
        int io = o.offset;
        while (i < filled) {
            if (array[i] != o.array[io]) {
                return false;
            }
            i++;
            io++;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        // TODO optimize
        int result = 1;
        for (int i = offset; i < filled; i++) {
            result = 31 * result + array[i];
        }
        return result;
    }
}
