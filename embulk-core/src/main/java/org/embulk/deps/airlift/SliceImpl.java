package org.embulk.deps.airlift;

class SliceImpl extends org.embulk.deps.airlift.Slice {
    SliceImpl(final io.airlift.slice.Slice subst) {
        this.subst = subst;
    }

    @Override
    public byte getByte(final int index) {
        return this.subst.getByte(index);
    }

    @Override
    public int getInt(final int index) {
        return this.subst.getInt(index);
    }

    @Override
    public long getLong(final int index) {
        return this.subst.getLong(index);
    }

    @Override
    public double getDouble(final int index) {
        return this.subst.getDouble(index);
    }

    @Override
    public void getBytes(final int index, final byte[] destination, final int destinationIndex, final int length) {
        this.subst.getBytes(index, destination, destinationIndex, length);
    }

    @Override
    public void setByte(final int index, final int value) {
        this.subst.setByte(index, value);
    }

    @Override
    public void setInt(final int index, final int value) {
        this.subst.setInt(index, value);
    }

    @Override
    public void setLong(final int index, final long value) {
        this.subst.setLong(index, value);
    }

    @Override
    public void setDouble(final int index, final double value) {
        this.subst.setDouble(index, value);
    }

    @Override
    public void setBytes(final int index, final byte[] source) {
        this.subst.setBytes(index, source);
    }

    private final io.airlift.slice.Slice subst;
}
