package org.embulk.deps.buffer;

import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

public final class SliceImpl extends org.embulk.deps.buffer.Slice {
    public SliceImpl(final byte[] array, final int offset, final int length) {
        this.slice = Slices.wrappedBuffer(array, offset, length);
    }

    public byte getByte(final int index) {
        return this.slice.getByte(index);
    }

    public void getBytes(final int index, final byte[] destination, final int destinationIndex, final int length) {
        this.slice.getBytes(index, destination, destinationIndex, length);
    }

    public double getDouble(final int index) {
        return this.slice.getDouble(index);
    }

    public int getInt(final int index) {
        return this.slice.getInt(index);
    }

    public long getLong(final int index) {
        return this.slice.getLong(index);
    }

    public void setByte(final int index, final int value) {
        this.slice.setByte(index, value);
    }

    public void setBytes(final int index, final byte[] source) {
        this.slice.setBytes(index, source);
    }

    public void setDouble(final int index, final double value) {
        this.slice.setDouble(index, value);
    }

    public void setInt(final int index, final int value) {
        this.slice.setInt(index, value);
    }

    public void setLong(final int index, final long value) {
        this.slice.setLong(index, value);
    }

    private final Slice slice;
}
