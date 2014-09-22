package org.quickload.record;

import java.util.Arrays;

public class RecordBuilder
{
    private final PageAllocator allocator;
    private final PageOutput output;
    private final int[] columnOffsets;
    private final int payloadOffset;

    private Page page = null;
    private int position = 0;
    private final byte[] nullBitSet;
    private int recordPayloadSize;

    RecordBuilder(PageAllocator allocator, Schema schema, PageOutput output)
    {
        this.allocator = allocator;
        this.output = output;
        this.columnOffsets = Page.columnOffsets(schema);
        this.payloadOffset = Page.payloadOffset(schema);
        this.nullBitSet = new byte[Page.nullBitSetSize(schema)];
    }

    public void startRecord()
    {
        if (page == null) {
            page = allocator.allocatePage(payloadOffset);
        }
        recordPayloadSize = 0;
        Arrays.fill(nullBitSet, (byte) 0);
    }

    public void addRecord()
    {
        int recordSize = payloadOffset + recordPayloadSize;
        page.setInt(position, recordSize);
        page.setBytes(position + 4, nullBitSet);
        position += recordSize;
        if (page.capacity() < position + payloadOffset) {
            flush();
        }
    }

    public void flush()
    {
        if (page != null) {
            page.limitLength(position);
            output.addPage(page);
            page = null;
            position = 0;
        }
    }

    public void setNull(int columnIndex)
    {
        nullBitSet[columnIndex >>> 3] |= (1 << (columnIndex & 7));
    }

    public void setByte(int columnIndex, byte value)
    {
        page.setByte(position + columnOffsets[columnIndex], value);
    }

    public void setShort(int columnIndex, short value)
    {
        page.setShort(position + columnOffsets[columnIndex], value);
    }

    public void setInt(int columnIndex, int value)
    {
        page.setInt(position + columnOffsets[columnIndex], value);
    }

    public void setLong(int columnIndex, long value)
    {
        page.setLong(position + columnOffsets[columnIndex], value);
    }

    public void setFloat(int columnIndex, float value)
    {
        page.setFloat(position + columnOffsets[columnIndex], value);
    }

    public void setDouble(int columnIndex, double value)
    {
        page.setDouble(position + columnOffsets[columnIndex], value);
    }

    public void setString(int columnIndex, String value)
    {
        int index = page.addStringReference(value);
        page.setInt(position + columnOffsets[columnIndex], index);
    }

    // TODO for custom types
    //public int getPayloadDataOffset()
    //{
    //    return payloadOffset + recordPayloadSize;
    //}

    // TODO for custom types
    //public Page addPayloadData(int size)
    //{
    //    // TODO check capacity
    //    recordPayloadSize += size;
    //    return page;
    //}
}
