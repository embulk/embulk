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

    public Page getPage()
    {
        return page;
    }

    public void setNull(int columnIndex)
    {
        nullBitSet[columnIndex >>> 3] |= (1 << (columnIndex & 7));
    }

    public int getFixedLengthPosition(int columnIndex)
    {
        return position + columnOffsets[columnIndex];
    }

    public void setVariableLengthIndex(int columnIndex, int indexData)
    {
        page.setInt(position + columnOffsets[columnIndex], indexData);
    }

    public int setVariableLengthIndex(int columnIndex, int indexData, int payloadSize)
    {
        page.setInt(position + columnOffsets[columnIndex], indexData);
        int offset = recordPayloadSize;
        recordPayloadSize += payloadSize;
        return offset;
    }
}
