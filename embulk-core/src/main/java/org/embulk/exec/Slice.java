/*
 * This file Slice.java is derived from Airlift's Slice with modifications:
 * - extracting only a portion required from Embulk,
 * - merging JvmUtil.java, Preconditions.java, SizeOf.java, and Slices.java,
 * - moving its Java package to org.embulk.exec, and
 * - aligning the coding style.
 *
 * A part of Airlift's Slice (Preconditions.java) is derived from Google Guava.
 * The following methods came originally from Google Guava:
 * - private static String badPositionIndex(long index, long size, String desc)
 * - public static void checkPositionIndexes(int start, int end, int size)
 * - private static String badPositionIndexes(int start, int end, int size)
 *
 * Airlift's Slice is licensed under the Apache License, Version 2.0.
 *
 * Google Guava is also licensed under the Apache License, Version 2.0.
 * - Copyright (C) 2007 The Guava Authors
 */

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.exec;

import java.lang.reflect.Field;
import java.util.Objects;
import sun.misc.Unsafe;

@SuppressWarnings("sunapi")
public final class Slice {
    /**
     * Base object for relative addresses.  If null, the address is an
     * absolute location in memory.
     */
    private final Object base;

    /**
     * If base is null, address is the absolute memory location of data for
     * this slice; otherwise, address is the offset from the base object.
     * This base plus relative offset addressing is taken directly from
     * the Unsafe interface.
     * <p>
     * Note: if base object is a byte array, this address Unsafe.ARRAY_BYTE_BASE_OFFSET,
     * since the byte array data starts AFTER the byte array object header.
     */
    private final long address;

    /**
     * Size of the slice
     */
    private final int size;

    /**
     * Creates a slice over the specified array range.
     *
     * @param offset the array position at which the slice begins
     * @param length the number of array positions to include in the slice
     */
    public static Slice wrappedBuffer(final byte[] array, final int offset, final int length) {
        if (length == 0) {
            return EMPTY_SLICE;
        }
        return new Slice(array, offset, length);
    }

    /**
     * Creates an empty slice.
     */
    private Slice() {
        this.base = null;
        this.address = 0;
        this.size = 0;
    }

    /**
     * Creates a slice over the specified array range.
     *
     * @param offset the array position at which the slice begins
     * @param length the number of array positions to include in the slice
     */
    private Slice(final byte[] base, final int offset, final int length) {
        Objects.requireNonNull(base, "base is null");
        checkPositionIndexes(offset, offset + length, base.length);

        this.base = base;
        this.address = Unsafe.ARRAY_BYTE_BASE_OFFSET + offset;
        this.size = length;
    }

    /**
     * Length of this slice.
     */
    public int length() {
        return this.size;
    }

    /**
     * Gets a byte at the specified absolute {@code index} in this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 1} is greater than {@code this.length()}
     */
    public byte getByte(final int index) {
        this.checkIndexLength(index, SIZE_OF_BYTE);
        return this.getByteUnchecked(index);
    }

    byte getByteUnchecked(final int index) {
        return Holder.unsafe.getByte(this.base, this.address + index);
    }

    /**
     * Gets a 32-bit integer at the specified absolute {@code index} in
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 4} is greater than {@code this.length()}
     */
    public int getInt(final int index) {
        this.checkIndexLength(index, SIZE_OF_INT);
        return this.getIntUnchecked(index);
    }

    int getIntUnchecked(final int index) {
        return Holder.unsafe.getInt(this.base, this.address + index);
    }

    /**
     * Gets a 64-bit long integer at the specified absolute {@code index} in
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 8} is greater than {@code this.length()}
     */
    public long getLong(final int index) {
        this.checkIndexLength(index, SIZE_OF_LONG);
        return getLongUnchecked(index);
    }

    long getLongUnchecked(final int index) {
        return Holder.unsafe.getLong(this.base, this.address + index);
    }

    /**
     * Gets a 64-bit double at the specified absolute {@code index} in
     * this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 8} is greater than {@code this.length()}
     */
    public double getDouble(final int index) {
        this.checkIndexLength(index, SIZE_OF_DOUBLE);
        return Holder.unsafe.getDouble(this.base, this.address + index);
    }

    /**
     * Transfers portion of data from this slice into the specified destination starting at
     * the specified absolute {@code index}.
     *
     * @param destinationIndex the first index of the destination
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     * if the specified {@code destinationIndex} is less than {@code 0},
     * if {@code index + length} is greater than
     * {@code this.length()}, or
     * if {@code destinationIndex + length} is greater than
     * {@code destination.length}
     */
    public void getBytes(final int index, final byte[] destination, final int destinationIndex, final int length) {
        this.checkIndexLength(index, length);
        checkPositionIndexes(destinationIndex, destinationIndex + length, destination.length);

        copyMemory(this.base, this.address + index, destination, (long) Unsafe.ARRAY_BYTE_BASE_OFFSET + destinationIndex, length);
    }

    /**
     * Sets the specified byte at the specified absolute {@code index} in this
     * buffer.  The 24 high-order bits of the specified value are ignored.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 1} is greater than {@code this.length()}
     */
    public void setByte(final int index, final int value) {
        this.checkIndexLength(index, SIZE_OF_BYTE);
        setByteUnchecked(index, value);
    }

    void setByteUnchecked(final int index, final int value) {
        Holder.unsafe.putByte(this.base, this.address + index, (byte) (value & 0xFF));
    }

    /**
     * Sets the specified 32-bit integer at the specified absolute
     * {@code index} in this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 4} is greater than {@code this.length()}
     */
    public void setInt(final int index, final int value) {
        this.checkIndexLength(index, SIZE_OF_INT);
        setIntUnchecked(index, value);
    }

    void setIntUnchecked(final int index, final int value) {
        Holder.unsafe.putInt(this.base, this.address + index, value);
    }

    /**
     * Sets the specified 64-bit long integer at the specified absolute
     * {@code index} in this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 8} is greater than {@code this.length()}
     */
    public void setLong(final int index, final long value) {
        this.checkIndexLength(index, SIZE_OF_LONG);
        setLongUnchecked(index, value);
    }

    void setLongUnchecked(final int index, final long value) {
        Holder.unsafe.putLong(this.base, this.address + index, value);
    }

    /**
     * Sets the specified 64-bit double at the specified absolute
     * {@code index} in this buffer.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     * {@code index + 8} is greater than {@code this.length()}
     */
    public void setDouble(final int index, final double value) {
        this.checkIndexLength(index, SIZE_OF_DOUBLE);
        Holder.unsafe.putDouble(this.base, this.address + index, value);
    }

    /**
     * Transfers data from the specified slice into this buffer starting at
     * the specified absolute {@code index}.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0}, or
     * if {@code index + source.length} is greater than {@code this.length()}
     */
    public void setBytes(final int index, final byte[] source) {
        setBytes(index, source, 0, source.length);
    }

    /**
     * Transfers data from the specified array into this buffer starting at
     * the specified absolute {@code index}.
     *
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     * if the specified {@code sourceIndex} is less than {@code 0},
     * if {@code index + length} is greater than
     * {@code this.length()}, or
     * if {@code sourceIndex + length} is greater than {@code source.length}
     */
    public void setBytes(final int index, final byte[] source, final int sourceIndex, final int length) {
        this.checkIndexLength(index, length);
        checkPositionIndexes(sourceIndex, sourceIndex + length, source.length);
        copyMemory(source, (long) Unsafe.ARRAY_BYTE_BASE_OFFSET + sourceIndex, this.base, this.address + index, length);
    }

    private static class Holder {  // Initialization-on-demand holder idiom.
        private static final Unsafe unsafe;

        static {
            try {
                // fetch theUnsafe object
                final Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                unsafe = (Unsafe) field.get(null);
                if (unsafe == null) {
                    throw new RuntimeException("sun.misc.Unsafe access not available");
                }

                // verify the stride of arrays matches the width of primitives
                assertArrayIndexScale("Byte", Unsafe.ARRAY_BYTE_INDEX_SCALE, 1);
                assertArrayIndexScale("Int", Unsafe.ARRAY_INT_INDEX_SCALE, 4);
                assertArrayIndexScale("Long", Unsafe.ARRAY_LONG_INDEX_SCALE, 8);
                assertArrayIndexScale("Double", Unsafe.ARRAY_DOUBLE_INDEX_SCALE, 8);
            } catch (final ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        private static void assertArrayIndexScale(final String name, final int actualIndexScale, final int expectedIndexScale) {
            if (actualIndexScale != expectedIndexScale) {
                throw new IllegalStateException(
                        name + " array index scale must be " + expectedIndexScale + ", but is " + actualIndexScale);
            }
        }
    }

    private static void copyMemory(final Object src, final long srcAddress, final Object dest, final long destAddress, final int length) {
        // The Unsafe Javadoc specifies that the transfer size is 8 iff length % 8 == 0
        // so ensure that we copy big chunks whenever possible, even at the expense of two separate copy operations
        int bytesToCopy = length - (length % 8);
        Holder.unsafe.copyMemory(src, srcAddress, dest, destAddress, bytesToCopy);
        Holder.unsafe.copyMemory(src, srcAddress + bytesToCopy, dest, destAddress + bytesToCopy, length - bytesToCopy);
    }

    private static String badPositionIndex(final long index, final long size, final String desc) {
        if (index < 0) {
            return String.format("%s (%s) must not be negative", desc, index);
        } else if (size < 0) {
            throw new IllegalArgumentException("negative size: " + size);
        } else { // index > size
            return String.format("%s (%s) must not be greater than size (%s)", desc, index, size);
        }
    }

    private static void checkPositionIndexes(final int start, final int end, final int size) {
        if (start < 0 || end < start || end > size) {
            throw new IndexOutOfBoundsException(badPositionIndexes(start, end, size));
        }
    }

    private static String badPositionIndexes(final int start, final int end, final int size) {
        if (start < 0 || start > size) {
            return badPositionIndex(start, size, "start index");
        }
        if (end < 0 || end > size) {
            return badPositionIndex(end, size, "end index");
        }
        // end < start
        return String.format("end index (%s) must not be less than start index (%s)", end, start);
    }

    private void checkIndexLength(final int index, final int length) {
        checkPositionIndexes(index, index + length, length());
    }

    /**
     * A slice with size {@code 0}.
     */
    private static final Slice EMPTY_SLICE = new Slice();

    private static final byte SIZE_OF_BYTE = 1;
    private static final byte SIZE_OF_INT = 4;
    private static final byte SIZE_OF_LONG = 8;
    private static final byte SIZE_OF_DOUBLE = 8;
}
