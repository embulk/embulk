package org.quickload.record;

import java.util.List;
import java.util.ArrayList;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;

public class Page
{
    ////
    //
    // Row
    // +---------------+------------+--------------------+------------------------+
    // | total-var-len | nul bitset | column header data | column var-len data... |
    // +---------------+------------+--------------------+------------------------+
    //  |                                     | |                      |
    //  +-------------------------------------+ +----------------------+
    //           fixed row data size             variable row data size

    // string:
    //   binary ref:       0 30-index (32-off 32-len)
    //   binary payload:   1 30-dataOff (32-dataLen)
    //   string ref:       2 30-index
    //   string payload:   3 30-dataOff (32-dataLen)
    //
    // binary:
    //   binary ref:       1 30-index (32-off 32-len)
    //   binary payload:   3 30-dataOff (32-dataLen)
    //

    private final Slice slice;
    private final List<String> stringReferences;
    private int length;
    // TODO private final List<byte[]> binaryReferences;

    Page(int capacity)
    {
        this.slice = Slices.allocate(capacity);
        this.length = capacity;
        this.stringReferences = new ArrayList<String>();
    }

    public static Page allocate(int capacity)
    {
        return new Page(capacity);
    }

    public void clear()
    {
        stringReferences.clear();
        length = slice.length();
    }

    static int nullBitSetSize(Schema schema)
    {
        return (schema.size() + 7) / 8;
    }

    static int rowHeaderSize(Schema schema)
    {
        return 4 + nullBitSetSize(schema);
    }

    static int payloadOffset(Schema schema)
    {
        return rowHeaderSize(schema) + schema.getFixedStorageSize();
    }

    static int[] columnOffsets(Schema schema)
    {
        int[] offsets = new int[schema.size()];

        if (!schema.isEmpty()) {
            offsets[0] = rowHeaderSize(schema);
            for (int i=0; i < schema.size()-1; i++) {
                offsets[i+1] = offsets[i] + schema.getColumnType(i).getFixedStorageSize();
            }
        }

        return offsets;
    }

    public int length()
    {
        return length;
    }

    public int capacity()
    {
        return slice.length();
    }

    void limitLength(int length)
    {
        this.length = length;
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

    public void setBytes(int pos, byte[] src, int dstIndex, int length)
    {
        slice.setBytes(pos, src, dstIndex, length);
    }

    public String getStringReference(int index)
    {
        return stringReferences.get(index);
    }

    public int addStringReference(String value)
    {
        int index = stringReferences.size();
        stringReferences.add(value);
        return index;
    }
}
