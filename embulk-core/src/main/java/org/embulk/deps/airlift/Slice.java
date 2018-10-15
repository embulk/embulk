package org.embulk.deps.airlift;

/**
 * Proxies callings to io.airlift.slice.Slices.
 *
 * <p>It works like a Singleton, just one instance in the entire Java runtime, not per Embulk's execution session.
 * Singleton is fine for this because use of the library should not vary per execution session.
 */
public abstract class Slice {
    public abstract byte getByte(final int index);

    public abstract int getInt(final int index);

    public abstract long getLong(final int index);

    public abstract double getDouble(final int index);

    public abstract void getBytes(final int index, final byte[] destination, final int destinationIndex, final int length);

    public abstract void setByte(final int index, final int value);

    public abstract void setInt(final int index, final int value);

    public abstract void setLong(final int index, final long value);

    public abstract void setDouble(final int index, final double value);

    public abstract void setBytes(final int index, final byte[] source);
}
