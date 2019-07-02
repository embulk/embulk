package org.embulk.spi;

import java.util.Arrays;

public class Buffer implements org.embulk.api.v0.Buffer {
    protected Buffer(final byte[] wrap, final int offset, final int capacity) {
        this.array = wrap;
        this.offset = offset;
        this.capacity = capacity;
        this.filled = offset;
        if (this.array.length < offset + capacity) {
            // TODO
            throw new IllegalStateException("capacity out of bound");
        }
    }

    public static org.embulk.spi.Buffer allocate(final int length) {
        return new org.embulk.spi.Buffer(new byte[length], 0, length);
    }

    public static org.embulk.spi.Buffer copyOf(final byte[] src) {
        return copyOf(src, 0, src.length);
    }

    public static org.embulk.spi.Buffer copyOf(final byte[] src, final int index, final int length) {
        return wrap(Arrays.copyOfRange(src, index, length));
    }

    public static org.embulk.spi.Buffer wrap(final byte[] src) {
        return wrap(src, 0, src.length);
    }

    public static org.embulk.spi.Buffer wrap(final byte[] src, final int offset, final int size) {
        return new org.embulk.spi.Buffer(src, offset, size).limit(size);
    }

    // http://findbugs.sourceforge.net/bugDescriptions.html#EI_EXPOSE_REP
    @Override  // From org.embulk.api.v0.Buffer
    public byte[] arrayUnsafe() {
        return this.array;
    }

    @Deprecated
    public byte[] array() {
        return this.arrayUnsafe();
    }

    @Override  // From org.embulk.api.v0.Buffer
    public int offset() {
        return this.offset;
    }

    @Override  // From org.embulk.api.v0.Buffer
    public org.embulk.spi.Buffer offset(final int offset) {
        this.offset = offset;
        return this;
    }

    @Override  // From org.embulk.api.v0.Buffer
    public int limit() {
        return this.filled - this.offset;
    }

    @Override  // From org.embulk.api.v0.Buffer
    public org.embulk.spi.Buffer limit(final int limit) {
        if (this.capacity < limit) {
            // TODO
            throw new IllegalStateException("limit index out of bound: capacity=" + this.capacity + " limit=" + limit);
        }
        this.filled = this.offset + limit;
        return this;
    }

    @Override  // From org.embulk.api.v0.Buffer
    public int capacity() {
        return this.capacity;
    }

    @Override  // From org.embulk.api.v0.Buffer
    public void setBytes(final int index, final byte[] source, final int sourceIndex, final int length) {
        System.arraycopy(source, sourceIndex, this.array, this.offset + index, length);
    }

    @Deprecated  // To provide a compatible method signature
    public void setBytes(final int index, final org.embulk.spi.Buffer source, final int sourceIndex, final int length) {
        this.setBytes(index, source.arrayUnsafe(), source.offset() + sourceIndex, length);
    }

    @Override  // From org.embulk.api.v0.Buffer
    public void setBytes(final int index, final org.embulk.api.v0.Buffer source, final int sourceIndex, final int length) {
        this.setBytes(index, source.arrayUnsafe(), source.offset() + sourceIndex, length);
    }

    @Override
    public void getBytes(final int index, final byte[] dest, final int destIndex, final int length) {
        System.arraycopy(this.array, this.offset + index, dest, destIndex, length);
    }

    @Deprecated  // To provide a compatible method signature
    public void getBytes(final int index, final org.embulk.spi.Buffer dest, final int destIndex, final int length) {
        this.getBytes(index, dest.arrayUnsafe(), dest.offset() + destIndex, length);
    }

    @Override  // From org.embulk.api.v0.Buffer
    public void getBytes(final int index, final org.embulk.api.v0.Buffer dest, final int destIndex, final int length) {
        this.getBytes(index, dest.arrayUnsafe(), dest.offset() + destIndex, length);
    }

    @Override  // From org.embulk.api.v0.Buffer
    public void release() {}

    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof Buffer)) {
            return false;
        }
        final Buffer other = (Buffer) otherObject;

        // TODO optimize
        if (this.limit() != other.limit()) {
            return false;
        }
        int ti = this.offset;
        int oi = other.offset;
        while (ti < this.filled) {
            if (this.array[ti] != other.array[oi]) {
                return false;
            }
            ti++;
            oi++;
        }
        return true;
    }

    @Override
    public int hashCode() {
        // TODO optimize
        int result = 1;
        for (int i = offset; i < filled; i++) {
            result = 31 * result + array[i];
        }
        return result;
    }

    public static final Buffer EMPTY = Buffer.allocate(0);

    private final byte[] array;
    private final int capacity;

    private int offset;
    private int filled;
}
